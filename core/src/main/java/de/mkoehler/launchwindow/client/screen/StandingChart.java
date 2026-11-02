package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Standing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The standing chart ({@code ui-spec.md} component 30): prestige, and under it the player's funds, on
 * one time axis, with a crosshair and tooltip through both. What it shows comes from {@link Standing};
 * this draws it. The lines go straight through the batch, the text is labels placed by {@link #layout}.
 *
 * @author Mario Koehler
 */
public final class StandingChart extends WidgetGroup {

    private static final float GUTTER_LEFT = 64f;
    private static final float GUTTER_RIGHT = 236f;
    private static final float HEAD_HEIGHT = 32f;
    private static final float AXIS_HEIGHT = 26f;
    private static final float TOP_ROOM = 8f;
    /** The prestige plot's share of the height both plots get: the mockup's 470 to 220. */
    private static final float PRESTIGE_SHARE = 0.68f;
    private static final float YEAR_GAP = 64f;
    private static final float LABEL_GAP = 34f;
    private static final float TICK = 5f;
    private static final float CROSSHAIR_REACH = 16f;
    private static final float TIP_WIDTH = 300f;
    private static final float DASH = 4f;
    private static final float GAP = 3f;

    private static final Color[] RIVALS = {Theme.CHART_RIVAL_1, Theme.CHART_RIVAL_2, Theme.CHART_RIVAL_3,
        Theme.CHART_RIVAL_4};

    /** One plot's place and scale. */
    private record Plot(float left, float right, float bottom, float top, long[] ticks) {

        float x(long turn, long from, long to) {
            return left + (right - left) * (turn - from) / (float) (to - from);
        }

        float y(long value) {
            long low = ticks[0];
            long high = ticks[ticks.length - 1];
            return bottom + (top - bottom) * (value - low) / (float) (high - low);
        }
    }

    private final UiKit ui;
    private final Standing.Model model;
    private final boolean everyone;
    private final boolean withFunds;
    private final int intervals;
    private final Actor prestigeHead;
    private final Actor fundsHead;
    private final Drawable pixel;
    private final List<float[]> leaders = new ArrayList<>();

    private Plot prestige;
    private Plot funds;
    private long hovered = -1L;
    private Table tip;
    private float builtWidth;
    private float builtHeight;

    /**
     * Builds a chart.
     *
     * @param ui           the UI kit
     * @param model        what it shows
     * @param everyone     whether the rivals' lines are on
     * @param withFunds    whether the funds plot shows
     * @param prestigeHead the row over the prestige plot, or {@code null}
     * @param fundsHead    the row over the funds plot, or {@code null}
     */
    public StandingChart(UiKit ui, Standing.Model model, boolean everyone, boolean withFunds, Actor prestigeHead,
                         Actor fundsHead) {
        this.ui = ui;
        this.model = model;
        this.everyone = everyone;
        this.withFunds = withFunds && model.funds() != null;
        this.intervals = this.withFunds ? 3 : 4;
        this.prestigeHead = prestigeHead;
        this.fundsHead = fundsHead;
        this.pixel = ui.solid(Color.WHITE);
        setTransform(false);
        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                if (prestige != null) {
                    float fraction = (x - prestige.left()) / (prestige.right() - prestige.left());
                    boolean inside = x >= prestige.left() - CROSSHAIR_REACH && x <= prestige.right() + CROSSHAIR_REACH;
                    hover(inside ? Standing.nearest(model.from(), model.to(), fraction) : -1L);
                }
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 && (toActor == null || !toActor.isDescendantOf(StandingChart.this))) {
                    hover(-1L);
                }
            }
        });
    }

    /**
     * Puts the crosshair on a turn, or takes it away.
     *
     * @param turn the turn, or -1
     */
    public void hover(long turn) {
        if (turn == hovered) {
            return;
        }
        hovered = turn;
        if (tip != null) {
            tip.remove();
            tip = null;
        }
        if (turn >= 0 && prestige != null) {
            tip = tooltip(turn);
            tip.setTouchable(Touchable.disabled);
            tip.setSize(TIP_WIDTH, tip.getPrefHeight());
            float crosshair = prestige.x(turn, model.from(), model.to());
            float x = crosshair + TIP_WIDTH + Theme.SPACE_4 <= getWidth() ? crosshair + Theme.SPACE_4
                : crosshair - Theme.SPACE_4 - TIP_WIDTH;
            float y = Math.max(0f, prestige.top() - Theme.SPACE_5 - tip.getHeight());
            tip.setPosition(x, y);
            addActor(tip);
        }
    }

    @Override
    public float getPrefWidth() {
        return 400f;
    }

    @Override
    public float getPrefHeight() {
        return 250f;
    }

    @Override
    public void layout() {
        if (getWidth() == builtWidth && getHeight() == builtHeight) {
            return;
        }
        builtWidth = getWidth();
        builtHeight = getHeight();
        clearChildren();
        leaders.clear();
        float left = GUTTER_LEFT;
        float right = getWidth() - GUTTER_RIGHT;
        float top = getHeight();
        if (prestigeHead != null) {
            place(prestigeHead, 0f, top - HEAD_HEIGHT, getWidth(), HEAD_HEIGHT);
            top -= HEAD_HEIGHT + Theme.SPACE_2;
        }
        float bottom = AXIS_HEIGHT;
        long[] prestigeTicks = ticks(model.lines());
        if (withFunds) {
            float fundsRow = fundsHead == null ? 0f : HEAD_HEIGHT + Theme.SPACE_3;
            float both = top - bottom - fundsRow;
            float prestigeBottom = top - both * PRESTIGE_SHARE;
            prestige = new Plot(left, right, prestigeBottom, top - TOP_ROOM, prestigeTicks);
            funds = new Plot(left, right, bottom, bottom + both * (1f - PRESTIGE_SHARE) - TOP_ROOM,
                ticks(List.of(model.funds())));
            if (fundsHead != null) {
                place(fundsHead, 0f, prestigeBottom - HEAD_HEIGHT - Theme.SPACE_2, getWidth(), HEAD_HEIGHT);
            }
            labels(funds, List.of(model.funds()), true);
        } else {
            prestige = new Plot(left, right, bottom, top - TOP_ROOM, prestigeTicks);
            funds = null;
        }
        List<Standing.Line> shown = shown();
        labels(prestige, shown, false);
        yearLabels();
        reviewLabels();
        if (hovered >= 0) {
            long turn = hovered;
            hovered = -1L;
            hover(turn);
        }
    }

    private void place(Actor actor, float x, float y, float width, float height) {
        actor.setBounds(x, y, width, height);
        addActor(actor);
    }

    private long[] ticks(List<Standing.Line> lines) {
        long low = Long.MAX_VALUE;
        long high = Long.MIN_VALUE;
        for (Standing.Line line : lines) {
            if (line.you() || everyone || line.colour() < 0) {
                for (long turn = model.from(); turn <= model.to(); turn++) {
                    if (line.has(turn)) {
                        low = Math.min(low, line.at(turn));
                        high = Math.max(high, line.at(turn));
                    }
                }
            }
        }
        return Standing.ticks(low, high, intervals);
    }

    private List<Standing.Line> shown() {
        return model.lines().stream().filter(line -> line.you() || everyone).toList();
    }

    private void labels(Plot plot, List<Standing.Line> lines, boolean money) {
        for (long tick : plot.ticks()) {
            Label label = text(number(tick), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
            label.setPosition(plot.left() - Theme.SPACE_2 - label.getWidth(), plot.y(tick) - label.getHeight() / 2f);
        }
        if (plot.ticks()[0] < 0L) {
            Label zero = text("Zero", Theme.TextStyle.CAPTION, Theme.INK_MUTED);
            zero.setPosition(plot.left() + Theme.SPACE_1, plot.y(0L) + 2f);
        }
        float[] wanted = new float[lines.size()];
        for (int i = 0; i < wanted.length; i++) {
            wanted[i] = plot.y(lines.get(i).at(lines.get(i).lastTurn()));
        }
        // A label stands half above and half below its place: keep all of it beside the plot.
        float half = LABEL_GAP / 2f;
        float[] placed = Standing.pushApart(wanted, LABEL_GAP, plot.bottom() + half,
            Math.max(plot.top() - half, plot.bottom() + half));
        for (int i = 0; i < wanted.length; i++) {
            endLabel(plot, lines.get(i), placed[i], money);
            if (Math.abs(placed[i] - wanted[i]) > 2f) {
                float end = plot.x(lines.get(i).lastTurn(), model.from(), model.to());
                leaders.add(new float[] {end + 3f, wanted[i], plot.right() + Theme.SPACE_3, placed[i]});
            }
        }
    }

    private void endLabel(Plot plot, Standing.Line line, float y, boolean money) {
        float x = plot.right() + Theme.SPACE_4;
        Label name = text(line.name(), Theme.TextStyle.BODY_SM, colour(line));
        name.setEllipsis(true);
        name.setWidth(Math.min(name.getWidth(), GUTTER_RIGHT - Theme.SPACE_4));
        name.setPosition(x, y + 1f);
        long value = line.at(line.lastTurn());
        if (money && value < 0L) {
            Table tag = new Table();
            tag.setBackground(ui.solid(Theme.DANGER));
            tag.pad(0f, 5f, 0f, 5f);
            tag.add(ui.label(number(value), Theme.TextStyle.BODY_STRONG, Theme.ON_FILL));
            tag.pack();
            tag.setPosition(x, y - tag.getHeight() + 1f);
            tag.setTouchable(Touchable.disabled);
            addActor(tag);
            return;
        }
        Label figure = text(number(value), Theme.TextStyle.BODY_STRONG, Theme.INK);
        figure.setPosition(x, y - figure.getHeight() + 1f);
    }

    private void yearLabels() {
        Plot lowest = funds == null ? prestige : funds;
        float width = lowest.right() - lowest.left();
        for (long turn : Standing.labelledTurns(model.from(), model.to(), width, YEAR_GAP)) {
            Label label = text(Standing.axisLabel(turn, model.points()), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
            float x = lowest.x(turn, model.from(), model.to()) - label.getWidth() / 2f;
            label.setPosition(Math.min(x, getWidth() - label.getWidth()), 2f);
        }
    }

    private void reviewLabels() {
        if (!withFunds) {
            return;
        }
        for (Standing.Review review : model.reviews()) {
            Label label = text("Review " + review.number(), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
            label.setPosition(prestige.x(review.turn(), model.from(), model.to()) + Theme.SPACE_1,
                prestige.top() - label.getHeight());
        }
    }

    private Label text(String text, Theme.TextStyle style, Color color) {
        Label label = ui.label(text, style, color);
        label.pack();
        label.setTouchable(Touchable.disabled);
        addActor(label);
        return label;
    }

    private static String number(long value) {
        return (value < 0L ? "−" : "") + String.format(Locale.US, "%,d", Math.abs(value));
    }

    private Table tooltip(long turn) {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        card.defaults().growX().left();
        card.add(ui.label(Standing.date(turn).toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_2).row();
        Standing.Line yours = model.yours();
        if (yours != null && yours.has(turn)) {
            card.add(tipRow(null, "Your prestige", number(yours.at(turn)), Theme.INK, Theme.TextStyle.BODY_SM)).row();
        }
        if (funds != null && model.funds().has(turn)) {
            long value = model.funds().at(turn);
            card.add(tipRow(null, "Your funds", (value < 0L ? "−$ " : "$ ") + number(Math.abs(value)),
                value < 0L ? Theme.DANGER : Theme.INK, Theme.TextStyle.BODY_SM)).row();
        }
        if (everyone) {
            card.add(rule()).padTop(Theme.SPACE_2).padBottom(Theme.SPACE_2).row();
            card.add(ui.label("EVERYONE'S PRESTIGE", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_1)
                .row();
            for (Standing.Line line : model.ranked(turn)) {
                card.add(tipRow(colour(line), line.name(), number(line.at(turn)), colour(line),
                    Theme.TextStyle.BODY_SM)).padBottom(2f).row();
            }
        }
        // The result's chart is about everyone: the player's own reviews and crews stay off it.
        List<Standing.Event> events = model.events(turn, everyone).stream().filter(event -> withFunds
            || event.kind() == Standing.EventKind.FIRST || event.kind() == Standing.EventKind.GOAL).toList();
        if (!events.isEmpty()) {
            card.add(rule()).padTop(Theme.SPACE_2).padBottom(Theme.SPACE_2).row();
            for (Standing.Event event : events) {
                Table row = new Table();
                row.add(glyph(event)).size(14f).top().padRight(Theme.SPACE_2).padTop(2f);
                row.add(ui.paragraph(event.text(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
                card.add(row).padBottom(2f).row();
            }
        }
        return card;
    }

    private Actor rule() {
        Image rule = new Image(ui.solid(Theme.NAVY_BORDER));
        rule.setHeight(Theme.BORDER_THIN);
        Table holder = new Table();
        holder.add(rule).growX().height(Theme.BORDER_THIN);
        return holder;
    }

    private Table tipRow(Color swatch, String name, String value, Color nameColour, Theme.TextStyle style) {
        Table row = new Table();
        if (swatch != null) {
            row.add(new Image(ui.solid(swatch))).size(12f, 3f).padRight(Theme.SPACE_2);
        }
        Label left = ui.label(name, style, swatch == null ? Theme.INK_MUTED : nameColour);
        left.setEllipsis(true);
        row.add(left).expandX().fillX().minWidth(0f).padRight(Theme.SPACE_3);
        Label right = ui.label(value, Theme.TextStyle.BODY_STRONG, swatch == null ? nameColour : Theme.INK);
        row.add(right).right();
        return row;
    }

    private Actor glyph(Standing.Event event) {
        return switch (event.kind()) {
            case FIRST -> new Image(ui.circle(Theme.GOLD, colour(event.colour()), 2.5f, 12));
            case GOAL -> new Image(ui.circle(colour(event.colour()), null, 0f, 11));
            case CREW -> glyph(ui, Glyph.Kind.CREW);
            case REVIEW -> glyph(ui, Glyph.Kind.REVIEW);
        };
    }

    /**
     * Builds the key under the chart: the four marks, and how old the chart is.
     *
     * @param ui   the UI kit
     * @param asOf the words at its right
     * @return the row
     */
    public static Table key(UiKit ui, String asOf) {
        Table key = new Table();
        keyItem(ui, key, new Image(ui.circle(Theme.NAVY, null, 0f, 11)), "A declared goal");
        keyItem(ui, key, new Image(ui.circle(Theme.GOLD, Theme.NAVY, 2.5f, 12)), "A First");
        keyItem(ui, key, glyph(ui, Glyph.Kind.CREW), "A crew of yours lost");
        keyItem(ui, key, glyph(ui, Glyph.Kind.REVIEW), "Your budget review");
        key.add().expandX();
        key.add(ui.label(asOf, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        return key;
    }

    private static void keyItem(UiKit ui, Table key, Actor glyph, String text) {
        key.add(glyph).size(14f).padRight(Theme.SPACE_1);
        key.add(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(Theme.SPACE_5);
    }

    private static Glyph glyph(UiKit ui, Glyph.Kind kind) {
        return new Glyph(ui, kind);
    }

    /** A mark that's a drawn shape: the crew's diamond, the review's dashes. */
    private static final class Glyph extends Widget {

        enum Kind {
            CREW, REVIEW
        }

        private final UiKit ui;
        private final Kind kind;

        Glyph(UiKit ui, Kind kind) {
            this.ui = ui;
            this.kind = kind;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float x = getX() + getWidth() / 2f;
            float y = getY() + getHeight() / 2f;
            if (kind == Kind.CREW) {
                ui.line(batch, x - 3.2f, y - 3.2f, x + 3.2f, y + 3.2f, 9f, Theme.INK, 0f);
            } else {
                for (float at = -7f; at < 7f; at += 5f) {
                    ui.line(batch, x, y + at, x, y + at + 3f, 1f, Theme.CHART_AXIS, 0f);
                }
            }
        }
    }

    private Color colour(Standing.Line line) {
        return colour(line.colour());
    }

    /**
     * Returns a line's colour.
     *
     * @param index a rival's colour index, or -1 for the player's
     * @return the colour
     */
    static Color colour(int index) {
        return index < 0 ? Theme.NAVY : RIVALS[index % Standing.RIVAL_COLOURS];
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        float ox = getX();
        float oy = getY();
        grid(batch, prestige, ox, oy);
        if (funds != null) {
            grid(batch, funds, ox, oy);
            debtFill(batch, ox, oy);
        }
        reviews(batch, ox, oy);
        for (float[] leader : leaders) {
            ui.line(batch, ox + leader[0], oy + leader[1], ox + leader[2], oy + leader[3], 1f, Theme.CHART_AXIS, 0f);
        }
        for (Standing.Line line : shown()) {
            polyline(batch, prestige, line, ox, oy, false);
        }
        if (funds != null) {
            polyline(batch, funds, model.funds(), ox, oy, true);
        }
        for (Standing.Line line : shown()) {
            marks(batch, line, ox, oy);
        }
        if (hovered >= 0) {
            crosshair(batch, ox, oy);
        }
        batch.setColor(Color.WHITE);
        super.draw(batch, parentAlpha);
    }

    private void rect(Batch batch, Color color, float x, float y, float width, float height) {
        batch.setColor(color);
        pixel.draw(batch, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private void grid(Batch batch, Plot plot, float ox, float oy) {
        for (long tick : plot.ticks()) {
            boolean zero = tick == 0L && plot.ticks()[0] < 0L;
            boolean base = tick == plot.ticks()[0];
            rect(batch, zero || base ? Theme.CHART_AXIS : Theme.CHART_GRID, ox + plot.left(), oy + plot.y(tick),
                plot.right() - plot.left(), zero ? 1.25f : 1f);
        }
        for (long winter : Standing.winters(model.from(), model.to())) {
            rect(batch, Theme.CHART_AXIS, ox + plot.x(winter, model.from(), model.to()), oy + plot.bottom() - TICK,
                1f, TICK);
        }
    }

    private void debtFill(Batch batch, float ox, float oy) {
        float zero = funds.y(0L);
        Standing.Line line = model.funds();
        long from = Math.max(model.from(), line.firstTurn());
        for (long turn = from; turn < line.lastTurn(); turn++) {
            float x1 = funds.x(turn, model.from(), model.to());
            float x2 = funds.x(turn + 1, model.from(), model.to());
            float y1 = funds.y(line.at(turn));
            float y2 = funds.y(line.at(turn + 1));
            if (y1 >= zero && y2 >= zero) {
                continue;
            }
            float start = (float) Math.floor(x1);
            for (float x = start; x < x2; x += 1f) {
                float across = Math.max(0f, Math.min(1f, (x + 0.5f - x1) / (x2 - x1)));
                float y = y1 + (y2 - y1) * across;
                if (y < zero) {
                    rect(batch, Theme.CHART_DEBT_FILL, ox + x, oy + y, 1f, zero - y);
                }
            }
        }
    }

    private void reviews(Batch batch, float ox, float oy) {
        if (funds == null) {
            return;
        }
        for (Standing.Review review : model.reviews()) {
            float x = ox + prestige.x(review.turn(), model.from(), model.to());
            dashed(batch, x, oy + prestige.bottom(), prestige.top() - prestige.bottom());
            dashed(batch, x, oy + funds.bottom(), funds.top() - funds.bottom());
        }
    }

    private void dashed(Batch batch, float x, float y, float height) {
        for (float at = 0f; at < height; at += DASH + GAP) {
            rect(batch, Theme.CHART_REVIEW, x, y + at, 1f, Math.min(DASH, height - at));
        }
    }

    private void polyline(Batch batch, Plot plot, Standing.Line line, float ox, float oy, boolean money) {
        float width = line.you() ? 3f : 1.75f;
        Color color = colour(line);
        boolean dashes = line.colour() >= Standing.RIVAL_COLOURS;
        long from = Math.max(model.from(), line.firstTurn());
        for (long turn = from; turn < line.lastTurn(); turn++) {
            float x1 = ox + plot.x(turn, model.from(), model.to());
            float y1 = oy + plot.y(line.at(turn));
            float x2 = ox + plot.x(turn + 1, model.from(), model.to());
            float y2 = oy + plot.y(line.at(turn + 1));
            if (dashes) {
                dashedLine(batch, x1, y1, x2, y2, width, color);
            } else if (money) {
                debtAwareLine(batch, plot, x1, y1, x2, y2, oy, width);
            } else {
                ui.line(batch, x1, y1, x2, y2, width, color, 0.5f);
            }
        }
        if (model.points() <= Standing.DOTS_UP_TO) {
            boolean big = model.points() <= Standing.BIG_DOTS_UP_TO;
            int size = line.you() ? (big ? 12 : 7) : (big ? 10 : 5);
            float ring = line.you() ? 1.5f : 1f;
            for (long turn = from; turn <= line.lastTurn(); turn++) {
                Color fill = money && line.at(turn) < 0L ? Theme.DANGER : color;
                Drawable dot = ui.circle(fill, Theme.SURFACE_CARD, ring, size + 2);
                float x = ox + plot.x(turn, model.from(), model.to());
                float y = oy + plot.y(line.at(turn));
                dot.draw(batch, x - (size + 2) / 2f, y - (size + 2) / 2f, size + 2, size + 2);
            }
        }
    }

    private void debtAwareLine(Batch batch, Plot plot, float x1, float y1, float x2, float y2, float oy, float width) {
        float zero = oy + plot.y(0L);
        if (y1 >= zero && y2 >= zero) {
            ui.line(batch, x1, y1, x2, y2, width, Theme.NAVY, 0.5f);
        } else if (y1 <= zero && y2 <= zero) {
            ui.line(batch, x1, y1, x2, y2, width, Theme.DANGER, 0.5f);
        } else {
            float cross = x1 + (x2 - x1) * (zero - y1) / (y2 - y1);
            ui.line(batch, x1, y1, cross, zero, width, y1 > zero ? Theme.NAVY : Theme.DANGER, 0.5f);
            ui.line(batch, cross, zero, x2, y2, width, y2 > zero ? Theme.NAVY : Theme.DANGER, 0.5f);
        }
    }

    private void dashedLine(Batch batch, float x1, float y1, float x2, float y2, float width, Color color) {
        float length = (float) Math.hypot(x2 - x1, y2 - y1);
        for (float at = 0f; at < length; at += 2f * DASH) {
            float end = Math.min(length, at + DASH);
            ui.line(batch, x1 + (x2 - x1) * at / length, y1 + (y2 - y1) * at / length,
                x1 + (x2 - x1) * end / length, y1 + (y2 - y1) * end / length, width, color, 0f);
        }
    }

    private void marks(Batch batch, Standing.Line line, float ox, float oy) {
        for (Standing.Event goal : line.goals()) {
            if (goal.turn() < model.from() || !line.has(goal.turn())) {
                continue;
            }
            float x = ox + prestige.x(goal.turn(), model.from(), model.to());
            float y = oy + prestige.y(line.at(goal.turn()));
            boolean first = goal.kind() == Standing.EventKind.FIRST;
            int size = first ? 14 : 12;
            Drawable mark = first ? ui.circle(Theme.GOLD, colour(line), 2.5f, size)
                : ui.circle(colour(line), Theme.SURFACE_CARD, 2f, size + 2);
            float drawn = first ? size : size + 2f;
            mark.draw(batch, x - drawn / 2f, y - drawn / 2f, drawn, drawn);
        }
        if (line.you() && withFunds) {
            for (long turn : model.lostCrews()) {
                if (line.has(turn)) {
                    float x = ox + prestige.x(turn, model.from(), model.to());
                    float y = oy + prestige.y(line.at(turn));
                    ui.line(batch, x - 3.2f, y - 3.2f, x + 3.2f, y + 3.2f, 9f, Theme.INK, 0f);
                }
            }
        }
    }

    private void crosshair(Batch batch, float ox, float oy) {
        float x = ox + prestige.x(hovered, model.from(), model.to());
        float top = oy + prestige.top() + TOP_ROOM;
        float bottom = oy + (funds == null ? prestige.bottom() : funds.bottom()) - TICK;
        rect(batch, Theme.NAVY, x, bottom, 1f, top - bottom);
        for (Standing.Line line : shown()) {
            if (line.has(hovered)) {
                ring(batch, x, oy + prestige.y(line.at(hovered)), colour(line));
            }
        }
        if (funds != null && model.funds().has(hovered)) {
            long value = model.funds().at(hovered);
            ring(batch, x, oy + funds.y(value), value < 0L ? Theme.DANGER : Theme.NAVY);
        }
    }

    private void ring(Batch batch, float x, float y, Color color) {
        Drawable ring = ui.circle(Theme.SURFACE_CARD, color, 2.5f, 13);
        ring.draw(batch, x - 6.5f, y - 6.5f, 13f, 13f);
    }
}
