package de.mkoehler.launchwindow.devtools.handson;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.HandsOnView;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.HandsOnLevel;
import de.mkoehler.launchwindow.rules.HandsOn;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The {@link HandsOnPlayground}'s list: every hands-on level in {@code decisions.json}, read again each
 * time the list shows, and for the one picked its start, its commands, the component it runs on, the lead
 * engineer's skill and the seed. Double-click a level or press Enter to play.
 *
 * @author Mario Koehler
 */
final class HandsOnChooser extends ScreenAdapter {

    private static final long DOUBLE_CLICK_MILLIS = 400;
    /** The lead's skills on offer: the fail chance runs from its highest at 0 to its floor. */
    private static final int[] SKILLS = {0, 20, 40, 60, 80, 100};
    private static final SplittableRandom SEEDS = new SplittableRandom();

    private final HandsOnPlayground app;
    private final UiKit ui;
    private final Stage stage = new Stage(new FitViewport(Theme.VIEW_WIDTH, Theme.VIEW_HEIGHT));
    private final Table root = new Table();
    private final List<Decisions.Decision> levels = new ArrayList<>();
    private Decisions.Decision picked;
    private String componentId;
    private int skill = 60;
    private long seed = newSeed();
    private String problem;
    private int fontGeneration;
    private long lastClickMillis;

    HandsOnChooser(HandsOnPlayground app) {
        this.app = app;
        this.ui = app.ui();
        root.setFillParent(true);
        root.setBackground(ui.blueprintGrid());
        stage.addActor(root);
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
     * A fresh seed, small enough to read and type.
     *
     * @return the seed
     */
    static long newSeed() {
        return SEEDS.nextLong(1, 100_000);
    }

    /**
     * Takes over the run last played, so the list shows it on return.
     *
     * @param run the run
     */
    void remember(HandsOnPlayground.Run run) {
        componentId = run.componentId();
        skill = run.skill();
        seed = run.seed();
        problem = null;
    }

    /**
     * Shows a problem above the list until the next run.
     *
     * @param text the problem
     */
    void problem(String text) {
        problem = text;
    }

    @Override
    public void show() {
        // Read the levels again: one may have changed since the last look.
        String keep = picked == null ? null : picked.id();
        levels.clear();
        try {
            app.reloadDecisions().decisions().stream().filter(d -> d.handsOn() != null).forEach(levels::add);
        } catch (RuntimeException e) {
            problem = "decisions.json doesn't read: " + e.getMessage();
        }
        picked = levels.stream().filter(d -> d.id().equals(keep)).findFirst()
            .orElse(levels.isEmpty() ? null : levels.getFirst());
        if (picked != null && !fits(picked, componentId)) {
            componentId = components(picked).getFirst();
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
        app.play(new HandsOnPlayground.Run(picked.id(), componentId, skill, seed));
    }

    /** The components a level can run on: a rocket, a payload, or either for one that fits any. */
    private static List<String> components(Decisions.Decision decision) {
        return switch (decision.fits()) {
            case ROCKET -> List.of(HandsOnPlayground.ROCKET);
            case PAYLOAD -> List.of(HandsOnPlayground.PAYLOAD);
            case ANY -> List.of(HandsOnPlayground.ROCKET, HandsOnPlayground.PAYLOAD);
        };
    }

    private static boolean fits(Decisions.Decision decision, String component) {
        return component != null && components(decision).contains(component);
    }

    private void rebuild() {
        root.clearChildren();
        root.pad(Theme.SPACE_5);
        root.top().left();

        Table head = new Table();
        Table titles = new Table();
        titles.defaults().left();
        titles.add(ui.label("DEV TOOLS", Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).row();
        titles.add(ui.label("Hands-on playground", Theme.TextStyle.HEADING_LG, Theme.ON_PAGE_STRONG));
        head.add(titles).expandX().left();
        root.add(head).growX().padBottom(Theme.SPACE_4).row();
        if (problem != null) {
            root.add(ui.notice(Theme.NoticeKind.DANGER, "Couldn't play that.", problem)).growX()
                .padBottom(Theme.SPACE_4).row();
        }

        Table columns = new Table();
        columns.add(levelList()).width(620f).growY().padRight(Theme.SPACE_5);
        columns.add(detail()).grow();
        root.add(columns).grow();
    }

    private Table levelList() {
        Table rows = new Table();
        rows.top();
        rows.defaults().growX();
        for (Decisions.Decision level : levels) {
            boolean active = level == picked;
            Table row = new Table();
            row.setBackground(active ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f), Theme.NAVY,
                Theme.BORDER_THIN) : ui.solid(Theme.SURFACE_CARD));
            row.pad(8f, 10f, 8f, 10f);
            Table words = new Table();
            words.defaults().left();
            words.add(ui.label(level.headline(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            words.add(ui.label(level.id() + " · " + level.fits().name().toLowerCase(java.util.Locale.ROOT),
                Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            row.add(words).expandX().left();
            row.setTouchable(Touchable.enabled);
            row.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // Every click rebuilds the rows, so the listener can't count taps: the chooser does.
                    long now = System.currentTimeMillis();
                    boolean twice = picked == level && now - lastClickMillis < DOUBLE_CLICK_MILLIS;
                    lastClickMillis = now;
                    if (twice) {
                        playPicked();
                        return;
                    }
                    picked = level;
                    if (!fits(level, componentId)) {
                        componentId = components(level).getFirst();
                    }
                    rebuild();
                }
            });
            rows.add(row).padBottom(2f).row();
        }
        if (levels.isEmpty()) {
            rows.add(ui.label("No hands-on level in decisions.json.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
                .left();
        }
        Table card = ui.card();
        card.top();
        card.add(ui.label(levels.size() + " hands-on levels", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left()
            .padBottom(Theme.SPACE_2).row();
        card.add(rows).grow();
        return card;
    }

    private Table detail() {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        if (picked == null) {
            card.add(ui.paragraph("Nothing to play.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            return card;
        }
        HandsOnLevel level = picked.handsOn();
        card.add(ui.label(picked.headline(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        card.add(ui.paragraph(HandsOnView.caption(level), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padBottom(4f).row();
        card.add(ui.paragraph("Starts with " + HandsOnView.target(level, level.start()) + ". "
            + level.commands().size() + " commands: " + String.join(", ", level.commands().stream()
            .map(HandsOnLevel.Command::name).toList()) + ".", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_4).row();

        card.add(ui.label("RUNS ON", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table on = new Table();
        on.left();
        for (String component : components(picked)) {
            on.add(choice(ContentNames.of(component), component.equals(componentId), () -> componentId = component))
                .padRight(Theme.SPACE_2);
        }
        card.add(on).padBottom(Theme.SPACE_4).row();

        card.add(ui.label("THE LEAD'S SKILL", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table skills = new Table();
        skills.left();
        for (int each : SKILLS) {
            skills.add(choice(String.valueOf(each), each == skill, () -> skill = each)).padRight(Theme.SPACE_2);
        }
        card.add(skills).padBottom(4f).row();
        card.add(ui.label("Each command: " + HandsOnView.percent(HandsOn.failChance(skill)) + " % fail, "
            + HandsOnView.percent(HandsOn.EXCEPTIONAL) + " % exceptional (one engineer on the team, so no "
            + "chemistry bonus).", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();

        card.add(ui.label("SEED", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table seeds = new Table();
        seeds.left();
        seeds.add(ui.label(String.valueOf(seed), Theme.TextStyle.HEADING_SM, Theme.INK)).padRight(Theme.SPACE_3);
        seeds.add(choice("New seed", false, () -> seed = newSeed()));
        card.add(seeds).padBottom(Theme.SPACE_5).row();

        Table actions = new Table();
        TextButton play = ui.button("Play", Theme.ButtonKind.PRIMARY);
        play.addListener(clicked(this::playPicked));
        actions.add(play).height(38f).padRight(Theme.SPACE_4);
        actions.add(ui.paragraph("On the puzzle: R plays the same seed again, N a new seed, Esc or Back comes "
            + "back here. The levels are read from assets/content/standard/decisions.json before every run.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().fillX().left();
        card.add(actions);
        return card;
    }

    private TextButton choice(String text, boolean on, Runnable pick) {
        TextButton button = ui.button(text, on ? Theme.ButtonKind.PRIMARY : Theme.ButtonKind.SECONDARY);
        button.addListener(clicked(() -> {
            pick.run();
            rebuild();
        }));
        return button;
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
