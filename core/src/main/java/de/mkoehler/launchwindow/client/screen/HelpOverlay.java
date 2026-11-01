package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.ConceptChip;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Explainers;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.HelpValues;

import java.util.List;
import java.util.Locale;

/**
 * The help ({@code ui-spec.md} component 23): a panel over the current screen, so closing it puts
 * the player back exactly where they were. A search field and a reset on top, the topics grouped on
 * the left, the selected topic's text on the right. Opened from a screen, it starts filtered to that
 * screen's topics ({@link Help.Context}), with a chip that says so; typing searches every topic, and
 * the reset shows them all.
 *
 * @author Mario Koehler
 */
public final class HelpOverlay {

    /** The panel's size: most of the 1920 by 1080 view. */
    static final float WIDTH = 1560f;
    static final float HEIGHT = 920f;
    private static final float LIST_WIDTH = 340f;
    /** The text column's widest, so lines stay readable on the wide panel. */
    private static final float TEXT_WIDTH = 860f;
    private static final float FIELD_HEIGHT = 36f;

    private final SessionScreen host;
    private final LaunchWindowGame game;
    private final UiKit ui;
    private final Help help = Help.standard();
    private final HelpValues values;
    private final Table scrim = new Table();
    private final TextField search;
    private final Table status = new Table();
    private final Table list = new Table();
    private final Table text = new Table();
    private final ScrollPane listScroll;
    private final ScrollPane textScroll;
    private Help.Scope context;
    private String selected;
    /** Set while the reset empties the field, so that doesn't count as the player typing. */
    private boolean resetting;

    /**
     * Builds the help over a screen.
     *
     * @param host    the screen it opens over
     * @param context where it was opened from, or {@code null} for every topic
     */
    HelpOverlay(SessionScreen host, Help.Scope context) {
        this.host = host;
        this.game = host.game;
        this.ui = host.ui;
        this.values = HelpValues.of(host.session.gameInfo().response().budgetSchedule(),
            host.session.catalog().buildings());
        this.context = context;
        search = ui.searchField("Search the rules");
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!resetting && !search.getText().isBlank()) {
                    // Typing searches every topic: the screen's filter is done with.
                    HelpOverlay.this.context = null;
                }
                refresh();
            }
        });
        listScroll = scroll(list);
        textScroll = scroll(text);

        scrim.setFillParent(true);
        scrim.setBackground(ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.55f)));
        scrim.setTouchable(Touchable.enabled);
        scrim.addListener(new ClickListener());
        scrim.add(panel()).size(WIDTH, HEIGHT);
        refresh();
    }

    /**
     * Returns the overlay's actor, for the screen to put on its stage.
     *
     * @return the actor
     */
    Table actor() {
        return scrim;
    }

    /**
     * Puts the keyboard in the search field.
     */
    void focus() {
        if (scrim.getStage() != null) {
            scrim.getStage().setKeyboardFocus(search);
        }
    }

    /**
     * Types into the search field, as the player would.
     *
     * @param query the text
     */
    public void search(String query) {
        search.setText(query);
        if (!query.isBlank()) {
            context = null;
        }
        refresh();
    }

    /**
     * Shows a topic, whether or not the list shows it.
     *
     * @param id the topic's id
     */
    public void select(String id) {
        selected = id;
        if (help.shown(search.getText(), context, values).stream().noneMatch(topic -> topic.id().equals(id))) {
            // A link to a topic the filter hides: show every topic, so the list can mark it.
            reset();
        } else {
            refresh();
        }
        textScroll.setScrollY(0f);
    }

    /**
     * Scrolls the topic list to its end, for the screenshot tool to show the last groups.
     */
    public void scrollListToEnd() {
        listScroll.layout();
        listScroll.setScrollPercentY(1f);
        listScroll.updateVisualScroll();
    }

    /**
     * Clears the search and the screen's filter: every topic again.
     */
    public void reset() {
        context = null;
        resetting = true;
        search.setText("");
        resetting = false;
        refresh();
    }

    private Table panel() {
        Table panel = new Table();
        panel.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        panel.pad(Theme.SPACE_5 + Theme.BORDER_EMPHASIS);
        panel.top();

        Table head = new Table();
        Table titles = new Table();
        titles.defaults().left();
        titles.add(ui.label("HELP", Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        titles.add(ui.label("Rules and mechanics", Theme.TextStyle.HEADING_SM, Theme.INK));
        head.add(titles).expandX().left();
        head.add(ui.label("F1 opens help anywhere · Esc closes it", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padRight(Theme.SPACE_4);
        head.add(iconButton("close", host::closeHelp)).size(32f);
        panel.add(head).growX().padBottom(Theme.SPACE_4).row();

        Table searchRow = new Table();
        Table magnifier = new Table();
        magnifier.left();
        Image glyph = new Image(ui.icon("search"), Scaling.fit);
        glyph.setColor(Theme.INK_MUTED);
        magnifier.add(glyph).size(16f).padLeft(Theme.SPACE_3);
        magnifier.setTouchable(Touchable.disabled);
        Stack field = new Stack(search, magnifier);
        searchRow.add(field).growX().height(FIELD_HEIGHT).padRight(Theme.SPACE_3);
        searchRow.add(ChromeScreen.onClick(ui.button("Show all topics", Theme.ButtonKind.SECONDARY), this::reset))
            .height(FIELD_HEIGHT);
        panel.add(searchRow).growX().padBottom(Theme.SPACE_3).row();

        panel.add(status).growX().height(24f).padBottom(Theme.SPACE_3).row();

        Table columns = new Table();
        columns.top();
        list.top().left();
        columns.add(listScroll).width(LIST_WIDTH).growY().top();
        columns.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).growY()
            .padLeft(Theme.SPACE_4).padRight(Theme.SPACE_6);
        text.top().left();
        columns.add(textScroll).grow().top();
        panel.add(columns).grow();
        return panel;
    }

    private Table iconButton(String icon, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        button.add(glyph).size(16f);
        button.setTouchable(Touchable.enabled);
        button.addListener(SessionScreen.clicked(action));
        return button;
    }

    private ScrollPane scroll(Table content) {
        ScrollPane scroll = new ScrollPane(content, new ScrollPane.ScrollPaneStyle());
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setScrollingDisabled(true, false);
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1 && scroll.getStage() != null) {
                    scroll.getStage().setScrollFocus(scroll);
                }
            }
        });
        return scroll;
    }

    /**
     * Builds the status line, the list and the text again from the current search and filter, as
     * when coming back from an explainer (its unread dot is gone).
     */
    void refresh() {
        String query = search.getText();
        List<Help.Topic> shown = help.shown(query, context, values);
        if (shown.stream().noneMatch(topic -> topic.id().equals(selected))) {
            // A context opens on its most important topic; otherwise the list's first.
            selected = shown.isEmpty() ? null : context != null && query.isBlank() ? context.topicIds().get(0)
                : shown.get(0).id();
        }
        buildStatus(query, shown.size());
        buildList(shown, query);
        buildText();
    }

    private void buildStatus(String query, int count) {
        status.clearChildren();
        status.left();
        if (context != null && query.isBlank()) {
            Table chip = new Table();
            chip.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f), Theme.NAVY,
                Theme.BORDER_THIN));
            chip.pad(3f, 10f, 3f, 10f);
            chip.add(ui.label(count + (count == 1 ? " topic" : " topics") + " for " + context.place(),
                Theme.TextStyle.LABEL, Theme.NAVY));
            status.add(chip).left();
            status.add(ui.label("Type to search every topic, or show them all.", Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).padLeft(Theme.SPACE_3).left();
        } else if (!query.isBlank()) {
            status.add(ui.label(count == 0 ? "No topic mentions \"" + query.trim() + "\"."
                : count + (count == 1 ? " topic mentions" : " topics mention") + " \"" + query.trim() + "\".",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left();
        } else {
            status.add(ui.label("All " + count + " topics.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left();
        }
        status.add().expandX();
    }

    private void buildList(List<Help.Topic> shown, String query) {
        list.clearChildren();
        list.defaults().growX().left();
        if (shown.isEmpty()) {
            list.add(ui.paragraph("Try another word, or show every topic.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).width(LIST_WIDTH - Theme.SPACE_3);
            return;
        }
        boolean first = true;
        for (String group : Help.groups(shown)) {
            list.add(ui.label(group.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED))
                .padTop(first ? 0f : Theme.SPACE_4).padBottom(Theme.SPACE_2).padLeft(10f).row();
            first = false;
            for (Help.Topic topic : shown) {
                if (topic.group().equals(group)) {
                    list.add(topicRow(topic)).padBottom(2f).row();
                }
            }
        }
    }

    private Table topicRow(Help.Topic topic) {
        boolean current = topic.id().equals(selected);
        Table row = new Table();
        row.setBackground(current ? ui.solid(Theme.NAVY) : ui.solid(Theme.SURFACE_CARD));
        row.pad(7f, 10f, 7f, 10f);
        row.add(ui.label(topic.title(), current ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY,
            current ? Theme.ON_FILL : Theme.INK)).expandX().left();
        row.setTouchable(Touchable.enabled);
        row.addListener(SessionScreen.clicked(() -> select(topic.id())));
        return row;
    }

    private void buildText() {
        text.clearChildren();
        text.defaults().left();
        Help.Topic topic = selected == null ? null : help.byId(selected).orElse(null);
        if (topic == null) {
            return;
        }
        float width = TEXT_WIDTH;
        text.add(ui.label(topic.group().toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY))
            .padBottom(2f).row();
        text.add(ui.paragraph(topic.title(), Theme.TextStyle.HEADING_LG, Theme.INK)).width(width)
            .padBottom(Theme.SPACE_4).row();
        Help.Line.Kind previous = null;
        for (String raw : topic.body()) {
            Help.Line line = Help.Line.of(raw);
            // A non-breaking space keeps "25 %" on one line (libGDX only wraps at plain spaces).
            String filled = values.fill(line.text()).replace(" %", " %");
            if (previous == Help.Line.Kind.BULLET && line.kind() != Help.Line.Kind.BULLET) {
                text.add().height(Theme.SPACE_2).row();
            }
            previous = line.kind();
            switch (line.kind()) {
                case HEADING -> text.add(ui.paragraph(filled, Theme.TextStyle.HEADING_SM, Theme.INK)).width(width)
                    .padTop(Theme.SPACE_3).padBottom(Theme.SPACE_2).row();
                case BULLET -> {
                    Table bullet = new Table();
                    bullet.top().left();
                    bullet.add(ui.label("•", Theme.TextStyle.BODY, Theme.NAVY)).top().width(18f).left();
                    bullet.add(ui.paragraph(filled, Theme.TextStyle.BODY, Theme.INK)).width(width - 18f).left();
                    text.add(bullet).width(width).padBottom(Theme.SPACE_1).row();
                }
                case PARAGRAPH -> text.add(ui.paragraph(filled, Theme.TextStyle.BODY, Theme.INK)).width(width)
                    .padBottom(Theme.SPACE_3).row();
            }
        }
        // A locked explainer would answer a theory of the day early: no chip.
        List<Explainers.Explainer> open = topic.explainers().stream()
            .flatMap(id -> Explainers.standard().byId(id).stream()).filter(e -> !game.explainerLocked(e)).toList();
        if (!topic.related().isEmpty() || !open.isEmpty()) {
            text.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(width).height(Theme.BORDER_THIN)
                .padTop(Theme.SPACE_3).padBottom(Theme.SPACE_3).row();
        }
        if (!topic.related().isEmpty()) {
            text.add(ui.label("SEE ALSO", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
            HorizontalGroup links = new HorizontalGroup();
            links.wrap().space(Theme.SPACE_2).wrapSpace(Theme.SPACE_2).rowLeft().left();
            for (String id : topic.related()) {
                help.byId(id).ifPresent(other -> links.addActor(ChromeScreen.onClick(
                    ui.button(other.title(), Theme.ButtonKind.SECONDARY), () -> select(other.id()))));
            }
            text.add(links).width(width).padBottom(Theme.SPACE_4).row();
        }
        // Not over an explainer: a second one would free the first one's drawings on closing.
        if (!open.isEmpty() && !(host instanceof ExplainerScreen)) {
            text.add(ui.label("HOW THE REAL THING WORKS", Theme.TextStyle.LABEL, Theme.INK_MUTED))
                .padBottom(Theme.SPACE_2).row();
            HorizontalGroup chips = new HorizontalGroup();
            chips.wrap().space(ConceptChip.GAP).wrapSpace(ConceptChip.GAP).rowLeft().left();
            for (Explainers.Explainer explainer : open) {
                chips.addActor(new ConceptChip(ui, explainer.title(), !game.explainerRead(explainer.id()),
                    // Visits the explainer and comes back to this very screen, help still open on this topic.
                    () -> game.visit(new ExplainerScreen(game, explainer, 0, () -> host))));
            }
            text.add(chips).width(width).row();
        }
    }
}
