package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.DrawingView;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Explainers;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * An explainer ({@code ui-spec.md} component 18): a short series of sheets drawn like an
 * engineering drawing. Each sheet has a drawing on the left, three numbered points on the right, a
 * gold "In Launch Window" strip that ties the idea to the game, and a title block. Previous and
 * Next step through the sheets; Close and Done go back to the screen that opened it. No app
 * chrome, as in the mockup: it reads like a page laid over the game.
 *
 * @author Mario Koehler
 */
public final class ExplainerScreen extends SessionScreen {

    private static final float POINTS_WIDTH = 600f;
    private static final float POINT_DISC = 30f;
    private static final float TITLE_BLOCK_WIDTH = 780f;
    private static final float BAR_SPAN = 560f;
    private static final float BAR_KEY_WIDTH = 210f;
    private static final Color IN_GAME_FILL = new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.14f);

    private final Explainers.Explainer explainer;
    private final Supplier<Screen> back;
    private int sheet;

    /**
     * Opens an explainer.
     *
     * @param game      the game, in a session
     * @param explainer the explainer
     * @param sheet     the sheet to start on, from 0
     * @param back      builds the screen to go back to
     */
    public ExplainerScreen(LaunchWindowGame game, Explainers.Explainer explainer, int sheet, Supplier<Screen> back) {
        super(game);
        this.explainer = explainer;
        this.back = back;
        this.sheet = Math.max(0, Math.min(sheet, explainer.sheets().size() - 1));
        // Opening any sheet clears its "new" dot on the concept chips and in the Museum.
        game.markExplainerRead(explainer.id());
        rebuild();
    }

    /**
     * Shows another sheet.
     *
     * @param index the sheet, from 0
     */
    public void showSheet(int index) {
        sheet = Math.max(0, Math.min(index, explainer.sheets().size() - 1));
        rebuild();
    }

    /**
     * Goes back to the screen that opened the explainer.
     */
    public void close() {
        game.setScreen(back.get());
    }

    @Override
    protected void goBack() {
        close();
    }

    @Override
    public void dispose() {
        super.dispose();
        // The full drawings are big; free them once the explainer is gone (the Museum's thumbnails stay).
        ui.releasePictures("explainers/", "-thumb.png");
    }

    @Override
    protected void rebuild() {
        Explainers.Sheet current = explainer.sheets().get(sheet);
        root.clearChildren();
        root.top().pad(Theme.SPACE_6, 40f, Theme.SPACE_6, 40f);
        root.defaults().growX();
        root.add(header(current)).row();
        root.add(sheet(current)).grow().padTop(18f).row();
        root.add(navigation()).padTop(18f);
    }

    private Table header(Explainers.Sheet current) {
        Table header = new Table();
        Table titles = new Table();
        titles.defaults().left();
        // A one-sheet explainer's eyebrow has no sheet count, as in the mockups.
        String count = explainer.sheets().size() == 1 ? ""
            : " · Sheet " + (sheet + 1) + " of " + explainer.sheets().size();
        titles.add(ui.label("Explainer · " + explainer.eyebrowTopic() + count, Theme.TextStyle.EYEBROW,
            Theme.GOLD_ON_PAGE)).padBottom(6f).row();
        titles.add(ui.label(current.title(), Theme.TextStyle.EXPLAINER_TITLE, Theme.ON_PAGE_STRONG));
        header.add(titles).expandX().left().bottom();
        Table close = new Table();
        close.setBackground(ui.solid(Theme.SURFACE_CARD));
        Image cross = new Image(ui.icon("close"), Scaling.fit);
        cross.setColor(Theme.NAVY);
        close.add(cross).size(20f);
        close.setTouchable(Touchable.enabled);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                close();
            }
        });
        header.add(close).size(40f).bottom();
        return header;
    }

    private Table sheet(Explainers.Sheet current) {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(28f, 32f, 20f, 32f);
        Table body = new Table();
        body.add(drawingColumn(current)).expand().left();
        body.add(points(current)).width(POINTS_WIDTH).top().padLeft(40f).padTop(Theme.SPACE_1);
        card.add(body).grow().row();
        Table foot = new Table();
        foot.add(inGame(current)).grow();
        foot.add(titleBlock(current)).width(TITLE_BLOCK_WIDTH).fillY().padLeft(24f);
        card.add(foot).growX().padTop(18f);
        return card;
    }

    private Table drawingColumn(Explainers.Sheet current) {
        Table column = new Table();
        column.top().left();
        column.defaults().left();
        Explainers.drawing(current.drawing()).ifPresent(drawing -> {
            Table frame = new Table();
            frame.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            frame.pad(Theme.BORDER_THIN);
            frame.add(new DrawingView(ui, current.drawing(), drawing));
            column.add(frame).row();
        });
        if (!current.legend().isEmpty()) {
            Table legend = new Table();
            for (Explainers.LegendEntry entry : current.legend()) {
                legend.add(new Image(ui.solid(entry.gold() ? Theme.GOLD : Theme.NAVY))).size(14f).padRight(6f);
                legend.add(ui.label(entry.text(), Theme.TextStyle.DRAWING_TEXT, Theme.INK)).padRight(22f);
            }
            column.add(legend).padTop(18f).row();
        }
        if (!current.bars().isEmpty()) {
            column.add(bars(current.bars())).padTop(Theme.SPACE_3).row();
        }
        return column;
    }

    private Table bars(List<Explainers.BarGroup> groups) {
        Table chart = new Table();
        chart.defaults().left();
        for (Explainers.BarGroup group : groups) {
            chart.add(ui.label(group.label().toUpperCase(Locale.ROOT), Theme.TextStyle.DRAWING_HEAD, Theme.NAVY))
                .width(BAR_KEY_WIDTH).padRight(20f).padBottom(14f);
            Table pair = new Table();
            pair.defaults().left();
            for (int i = 0; i < group.rows().size(); i++) {
                Explainers.BarRow row = group.rows().get(i);
                Table line = new Table();
                float length = (float) Math.max(2.0, BAR_SPAN * row.value() / group.max());
                line.add(new Image(ui.solid(i == 0 ? Theme.NAVY : Theme.GOLD))).size(length, 14f).padRight(10f);
                line.add(ui.label(row.text(), Theme.TextStyle.BODY_STRONG, Theme.INK));
                pair.add(line).padBottom(i + 1 < group.rows().size() ? 4f : 0f).row();
            }
            chart.add(pair).padBottom(14f).row();
        }
        return chart;
    }

    private Table points(Explainers.Sheet current) {
        Table points = new Table();
        points.top().left();
        for (int i = 0; i < current.points().size(); i++) {
            Explainers.Point point = current.points().get(i);
            boolean last = i == current.points().size() - 1;
            Table disc = new Table();
            disc.setBackground(ui.circle(last ? Theme.GOLD : Theme.NAVY, null, 0f, (int) POINT_DISC));
            disc.add(ui.label(String.valueOf(i + 1), Theme.TextStyle.DRAWING_LABEL,
                last ? Theme.NAVY : Theme.ON_FILL));
            Table text = new Table();
            text.defaults().left().growX();
            text.add(ui.label(point.title(), Theme.TextStyle.POINT_TITLE, Theme.NAVY)).padBottom(6f).row();
            text.add(ui.paragraph(point.text(), Theme.TextStyle.READING, Theme.INK))
                .width(POINTS_WIDTH - POINT_DISC - 14f).row();
            if (point.drawing() != null) {
                Explainers.drawing(point.drawing()).ifPresent(drawing ->
                    text.add(new DrawingView(ui, point.drawing(), drawing)).fill(false, false).expand(false, false).left()
                        .padTop(10f));
            }
            points.add(disc).size(POINT_DISC).top().padRight(14f);
            points.add(text).top().left().padBottom(26f).row();
        }
        return points;
    }

    private Table inGame(Explainers.Sheet current) {
        Table strip = new Table();
        strip.setBackground(ui.rect(IN_GAME_FILL, Theme.GOLD, Theme.BORDER_THIN));
        strip.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        Image star = new Image(ui.icon("prestige"), Scaling.fit);
        star.setColor(Theme.GOLD_TEXT);
        strip.add(star).size(20f).top().padTop(2f).padRight(Theme.SPACE_3);
        Table text = new Table();
        text.defaults().left().growX();
        text.add(ui.label("In Launch Window", Theme.TextStyle.EYEBROW, Theme.GOLD_TEXT)).padBottom(3f).row();
        text.add(ui.paragraph(current.inGame(), Theme.TextStyle.CAPTION_LG, Theme.INK));
        strip.add(text).grow().top();
        return strip;
    }

    private Table titleBlock(Explainers.Sheet current) {
        Table block = new Table();
        block.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        block.pad(Theme.BORDER_EMPHASIS);
        String[][] cells = {
            {"Drawing", "Launch Window · " + explainer.code()},
            {"Title", explainer.title()},
            {"Sheet", (sheet + 1) + " of " + explainer.sheets().size()},
            {"Scale", current.scale()},
        };
        float[] shares = {1.4f, 2f, 0.8f, 1f};
        float inner = TITLE_BLOCK_WIDTH - 2 * Theme.BORDER_EMPHASIS - 3 * Theme.BORDER_THIN;
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                block.add(new Image(ui.solid(Theme.NAVY))).width(Theme.BORDER_THIN).growY();
            }
            Table cell = new Table();
            cell.left();
            cell.pad(6f, 10f, 6f, 10f);
            cell.defaults().left();
            cell.add(ui.label(cells[i][0].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED))
                .padBottom(2f).row();
            cell.add(ui.label(cells[i][1], Theme.TextStyle.DRAWING_LABEL_SM, Theme.NAVY));
            block.add(cell).width(inner * shares[i] / 5.2f).growY().left();
        }
        return block;
    }

    private Table navigation() {
        Table nav = new Table();
        List<Explainers.Sheet> sheets = explainer.sheets();
        TextButton previous = ui.pageButton(sheet == 0 ? "‹ Previous"
            : "‹ Previous: " + sheets.get(sheet - 1).title());
        if (sheet == 0) {
            TextButton.TextButtonStyle muted = new TextButton.TextButtonStyle(previous.getStyle());
            muted.fontColor = Theme.INK_MUTED;
            previous.setStyle(muted);
            previous.setDisabled(true);
        } else {
            onClick(previous, () -> showSheet(sheet - 1));
        }
        nav.add(previous).left();
        Table dots = new Table();
        for (int i = 0; i < sheets.size(); i++) {
            int index = i;
            Image dot = new Image(ui.circle(i == sheet ? Theme.ON_PAGE_STRONG : new Color(0f, 0f, 0f, 0f),
                Theme.ON_PAGE_STRONG, 1.5f, 10));
            dot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showSheet(index);
                }
            });
            dots.add(dot).size(10f).pad(0f, 4f, 0f, 4f);
        }
        nav.add(dots).expandX();
        boolean last = sheet == sheets.size() - 1;
        TextButton next = ui.button(last ? "Done" : "Next: " + sheets.get(sheet + 1).title() + " ›",
            Theme.ButtonKind.PRIMARY);
        onClick(next, last ? this::close : () -> showSheet(sheet + 1));
        nav.add(next).right();
        return nav;
    }

    private static void onClick(TextButton button, Runnable action) {
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
    }
}
