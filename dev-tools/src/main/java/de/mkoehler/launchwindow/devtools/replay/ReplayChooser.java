package de.mkoehler.launchwindow.devtools.replay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.client.view.ReplayClips;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The {@link MissionReplayer}'s list: every mission season in the pack, how many of its steps have a
 * clip, and for the one picked its steps with the variants each has, the outcome to play and the
 * step it goes on. Double-click a mission or press Enter to play.
 *
 * @author Mario Koehler
 */
final class ReplayChooser extends ScreenAdapter {

    private static final String[] VARIANT_LETTERS = {"S", "G", "F", "C"};
    private static final long DOUBLE_CLICK_MILLIS = 400;
    private static final String[] OUTCOME_NAMES = {"Success", "Glitch", "Failure", "Catastrophic"};

    /** One mission season in the list. */
    private record Entry(MissionConfiguration config, int season, String name, int steps, int withClips) {
    }

    private final MissionReplayer app;
    private final ContentCatalog catalog;
    private final Path assets;
    private final UiKit ui;
    private final Stage stage = new Stage(new FitViewport(Theme.VIEW_WIDTH, Theme.VIEW_HEIGHT));
    private final Table root = new Table();
    private final TextField search;
    private final List<Entry> entries = new ArrayList<>();
    private ReplayClips clips;
    private boolean clipsOnly;
    private Entry picked;
    private MissionStepOutcome outcome = MissionStepOutcome.SUCCESSFUL;
    private int target = -1;
    private int fontGeneration;
    private long lastClickMillis;
    private ScrollPane list;
    private float listScroll;

    ReplayChooser(MissionReplayer app, ContentCatalog catalog, Path assets) {
        this.app = app;
        this.catalog = catalog;
        this.assets = assets;
        this.ui = app.ui();
        root.setFillParent(true);
        root.setBackground(ui.blueprintGrid());
        stage.addActor(root);
        search = ui.searchField("Filter missions…");
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                listScroll = 0f;
                rebuild();
                stage.setKeyboardFocus(search);
            }
        });
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER && picked != null) {
                    playPicked();
                    return true;
                }
                return false;
            }
        });
        fontGeneration = ui.fonts().generation();
    }

    /**
     * Takes over the outcome the replay last played, so the list shows it on return.
     *
     * @param choice the choice
     */
    void remember(MissionReplayer.Choice choice) {
        outcome = choice.outcome();
    }

    /**
     * Picks a mission season and a step, as clicks would (the smoke test).
     *
     * @param configId the mission
     * @param season   the season
     * @param step     the step, or -1 for every step
     */
    void pick(String configId, int season, int step) {
        picked = entries.stream().filter(e -> e.config().id().equals(configId) && e.season() == season).findFirst()
            .orElse(null);
        target = step;
        rebuild();
    }

    @Override
    public void show() {
        // Read the clips again: one may have landed since the last look.
        clips = ReplayClips.load(assets);
        entries.clear();
        for (MissionConfiguration config : catalog.missionConfigurations()) {
            for (int season = 0; season < config.seasons().size(); season++) {
                List<MissionStep> steps = config.seasons().get(season).steps();
                int withClips = 0;
                for (int i = 0; i < steps.size(); i++) {
                    // The steps before pick the clip too (a landing on the Moon or on a runway, a descent at Mars).
                    withClips += clips.clipName(steps.get(i).id(), ids(steps, i), later(steps, i), config.programId())
                        .isPresent() ? 1 : 0;
                }
                String name = MissionControl.missionName(config) + (config.seasons().size() > 1
                    ? " · season " + (season + 1) + " of " + config.seasons().size() : "");
                entries.add(new Entry(config, season, name, steps.size(), withClips));
            }
        }
        if (picked != null) {
            Entry old = picked;
            picked = entries.stream().filter(e -> e.config().id().equals(old.config().id()) && e.season() == old.season())
                .findFirst().orElse(null);
        }
        rebuild();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(Theme.SURFACE_PAGE.r, Theme.SURFACE_PAGE.g, Theme.SURFACE_PAGE.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (fontGeneration != ui.fonts().generation()) {
            fontGeneration = ui.fonts().generation();
            rebuild();
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

    private void playPicked() {
        app.play(new MissionReplayer.Choice(picked.config(), picked.season(), outcome, target), 0);
    }

    private void rebuild() {
        if (list != null) {
            listScroll = list.getScrollY();
        }
        root.clearChildren();
        root.pad(Theme.SPACE_5);
        root.top().left();

        Table head = new Table();
        Table titles = new Table();
        titles.defaults().left();
        titles.add(ui.label("DEV TOOLS", Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).row();
        titles.add(ui.label("Mission replayer", Theme.TextStyle.HEADING_LG, Theme.ON_PAGE_STRONG));
        head.add(titles).expandX().left();
        head.add(toggle(clipsOnly ? "Showing: with clips" : "Showing: all missions", () -> {
            clipsOnly = !clipsOnly;
            listScroll = 0f;
            rebuild();
        })).padRight(Theme.SPACE_3);
        head.add(search).width(360f);
        root.add(head).growX().padBottom(Theme.SPACE_4).row();

        Table columns = new Table();
        columns.add(missionList()).width(720f).growY().padRight(Theme.SPACE_5);
        columns.add(detail()).grow();
        root.add(columns).grow();
    }

    private Actor toggle(String text, Runnable action) {
        TextButton button = ui.pageButton(text);
        button.addListener(clicked(action));
        return button;
    }

    private Table missionList() {
        String filter = search.getText().trim().toLowerCase(Locale.ROOT);
        Table rows = new Table();
        rows.top();
        rows.defaults().growX();
        int shown = 0;
        for (Entry entry : entries) {
            if (clipsOnly && entry.withClips() == 0
                || !filter.isEmpty() && !(entry.name() + " " + entry.config().id()).toLowerCase(Locale.ROOT).contains(filter)) {
                continue;
            }
            shown++;
            boolean active = entry == picked;
            Table row = new Table();
            row.setBackground(active ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f), Theme.NAVY,
                Theme.BORDER_THIN) : ui.solid(Theme.SURFACE_CARD));
            row.pad(6f, 10f, 6f, 10f);
            row.add(ui.label(entry.name(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            row.add(ui.label(entry.withClips() + " / " + entry.steps() + " clips", Theme.TextStyle.LABEL,
                entry.withClips() == 0 ? Theme.INK_MUTED : entry.withClips() == entry.steps() ? Theme.SUCCESS
                    : Theme.GOLD_TEXT));
            row.setTouchable(Touchable.enabled);
            row.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // Every click rebuilds the rows, so the listener can't count taps: the chooser does.
                    long now = System.currentTimeMillis();
                    boolean twice = picked == entry && now - lastClickMillis < DOUBLE_CLICK_MILLIS;
                    lastClickMillis = now;
                    if (twice) {
                        playPicked();
                        return;
                    }
                    if (picked != entry) {
                        picked = entry;
                        target = -1;
                    }
                    rebuild();
                }
            });
            rows.add(row).padBottom(2f).row();
        }
        if (shown == 0) {
            rows.add(ui.label("No mission matches.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).left();
        }
        Table card = ui.card();
        card.top();
        card.add(ui.label(shown + " mission seasons", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left()
            .padBottom(Theme.SPACE_2).row();
        list = scroll(rows);
        float keep = listScroll;
        card.add(list).grow();
        Gdx.app.postRunnable(() -> {
            list.setScrollY(keep);
            list.updateVisualScroll();
        });
        return card;
    }

    private Table detail() {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        if (picked == null) {
            card.add(ui.paragraph("Pick a mission on the left. Double-click it, or press Enter, to play its replay.\n\n"
                + "In the replay: 1 to 4 switch the outcome (success, glitch, failure, catastrophic) at the step "
                + "showing, R plays it again from the start, Esc or Back comes back here. Clips and sounds are read "
                + "from assets/ before every replay.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).growX();
            return card;
        }
        card.add(ui.label(picked.name(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
        card.add(ui.label(picked.config().id(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();

        card.add(ui.label("OUTCOME", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table outcomes = new Table();
        outcomes.left();
        for (int i = 0; i < MissionReplayer.OUTCOMES.size(); i++) {
            MissionStepOutcome each = MissionReplayer.OUTCOMES.get(i);
            TextButton button = ui.button((i + 1) + "  " + OUTCOME_NAMES[i],
                each == outcome ? Theme.ButtonKind.PRIMARY : Theme.ButtonKind.SECONDARY);
            button.addListener(clicked(() -> {
                outcome = each;
                rebuild();
            }));
            outcomes.add(button).height(34f).padRight(Theme.SPACE_2);
        }
        card.add(outcomes).padBottom(Theme.SPACE_4).row();

        card.add(ui.label("ON WHICH STEP", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table steps = new Table();
        steps.top();
        steps.defaults().growX();
        steps.add(stepRow(-1, "Every step", "the outcome on all of them; a failure doesn't end the season", null, null, null))
            .padBottom(2f).row();
        List<MissionStep> plan = picked.config().seasons().get(picked.season()).steps();
        for (int i = 0; i < plan.size(); i++) {
            String id = plan.get(i).id();
            List<String> before = ids(plan, i);
            List<String> after = later(plan, i);
            steps.add(stepRow(i, (i + 1) + "  " + TurnReportView.stepName(picked.config().id(), id),
                clips.clipName(id, before, after, picked.config().programId()).map(name -> "clip " + name)
                    .orElse("no clip: no signal"), id, before, after))
                .padBottom(2f).row();
        }
        card.add(scroll(steps)).grow().padBottom(Theme.SPACE_4).row();

        Table actions = new Table();
        TextButton play = ui.button("Play replay", Theme.ButtonKind.PRIMARY);
        play.addListener(clicked(this::playPicked));
        actions.add(play).height(38f).padRight(Theme.SPACE_4);
        actions.add(ui.label("Variants with their own clip: S success, G glitch, F failure, C catastrophic "
            + "(otherwise it plays the failure).", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        card.add(actions);
        return card;
    }

    /** The ids of a season's steps before the index-th. */
    private static List<String> ids(List<MissionStep> steps, int index) {
        return steps.subList(0, index).stream().map(MissionStep::id).toList();
    }

    /** The ids of a season's steps after the index-th. */
    private static List<String> later(List<MissionStep> steps, int index) {
        return steps.subList(index + 1, steps.size()).stream().map(MissionStep::id).toList();
    }

    private Table stepRow(int index, String name, String note, String stepId, List<String> before,
                          List<String> after) {
        Table row = new Table();
        boolean active = index == target;
        row.setBackground(active ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f), Theme.NAVY,
            Theme.BORDER_THIN) : ui.rect(Theme.SURFACE_CARD, new Color(Theme.INK_MUTED).mul(1f, 1f, 1f, 0.25f),
            Theme.BORDER_THIN));
        row.pad(5f, 10f, 5f, 10f);
        row.add(ui.label(name, Theme.TextStyle.BODY_SM, Theme.INK)).left();
        row.add(ui.label(note, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left().padLeft(Theme.SPACE_3);
        String program = picked == null ? null : picked.config().programId();
        if (stepId != null && clips.clipName(stepId, before, after, program).isPresent()) {
            String name0 = clips.clipName(stepId, before, after, program).get();
            for (int v = 0; v < VARIANT_LETTERS.length; v++) {
                MissionStepOutcome each = MissionReplayer.OUTCOMES.get(v);
                boolean own = clips.clipFor(stepId, before, after, program, each).video()
                    .endsWith(name0 + "-" + ReplayClips.variant(each) + ".webm");
                row.add(ui.label(VARIANT_LETTERS[v], Theme.TextStyle.LABEL, own ? Theme.SUCCESS
                    : new Color(Theme.INK_MUTED).mul(1f, 1f, 1f, 0.35f))).width(14f).padLeft(4f);
            }
        }
        row.setTouchable(Touchable.enabled);
        row.addListener(clicked(() -> {
            target = index;
            rebuild();
        }));
        return row;
    }

    private ScrollPane scroll(Table content) {
        ScrollPane pane = new ScrollPane(content, new ScrollPane.ScrollPaneStyle());
        pane.setFadeScrollBars(false);
        pane.setOverscroll(false, false);
        pane.setScrollingDisabled(true, false);
        pane.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    stage.setScrollFocus(pane);
                }
            }
        });
        return pane;
    }

    private static ClickListener clicked(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        };
    }
}
