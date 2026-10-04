package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * The base of every in-game screen except the hub ({@code ui-spec.md} &sect;2, {@code design.md}
 * &sect;3.13): the header (back, home, settings, the breadcrumb, and the prestige, game-date and
 * next-turn chips), the screen's eyebrow and title with room for its buttons, the content area,
 * and the footer's five stat chips. The toast, dialogs and the submit-then-rebuild loop come from
 * {@link SessionScreen}.
 *
 * <p>Subclasses fill {@link #content} in {@link #rebuild()}, which runs on creation and after every
 * state change, and may add buttons to {@link #titleActions}.</p>
 *
 * @author Mario Koehler
 */
abstract class ChromeScreen extends SessionScreen {

    private static final float NAV_BUTTON = 32f;

    /**
     * One breadcrumb step: its label, and where clicking it goes ({@code null} for the current one).
     * The action takes the screen, so a subclass can name it in its {@code super(...)} call.
     *
     * @param label  the label
     * @param action where it goes, such as {@code ChromeScreen::goHome}
     */
    protected record Crumb(String label, java.util.function.Consumer<ChromeScreen> action) {

        /** The breadcrumb's root, the Space Complex hub. */
        static final Crumb SPACE_COMPLEX = new Crumb("Space Complex", ChromeScreen::goHome);
        /** Headquarters' landing screen. */
        static final Crumb HEADQUARTERS = new Crumb("Headquarters", ChromeScreen::goHeadquarters);
        /** The SET Centre's personnel overview. */
        static final Crumb SET_CENTRE = new Crumb("SET Centre", ChromeScreen::goSetCentre);
        /** The VAB's landing screen. */
        static final Crumb VAB = new Crumb("VAB", ChromeScreen::goVab);
        /** The VAB's Manage Rocket Programs. */
        static final Crumb ROCKET_PROGRAMS = new Crumb("Manage Rocket Programs",
            screen -> screen.game.setScreen(new RocketProgramsScreen(screen.game)));
    }

    /** The screen's own area, between the title row and the footer. */
    protected final Table content = new Table();
    /** Room for the screen's buttons, right of its title. */
    protected final Table titleActions = new Table();

    private final Label prestige;
    private final Label gameDate;
    private final Label countdown;
    private final Table footer = new Table();

    /**
     * Builds the chrome.
     *
     * @param game    the game, in a session
     * @param crumbs  the breadcrumb, root first; the last one is the current screen
     * @param eyebrow the small label above the title
     * @param title   the screen's title
     */
    protected ChromeScreen(LaunchWindowGame game, List<Crumb> crumbs, String eyebrow, String title) {
        super(game);
        prestige = ui.label("", Theme.TextStyle.LABEL, Theme.NAVY);
        gameDate = ui.label("", Theme.TextStyle.LABEL, Theme.ON_FILL);
        countdown = ui.label("", Theme.TextStyle.LABEL, Theme.NAVY);

        root.top().pad(Theme.SPACE_6);
        root.defaults().growX();
        root.add(headerBar(crumbs)).row();
        root.add(line()).height(Theme.BORDER_THIN).padTop(Theme.SPACE_2).row();
        Table titleRow = new Table();
        Table titles = new Table();
        titles.defaults().left();
        titles.add(ui.label(eyebrow, Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE)).padBottom(Theme.SPACE_1).row();
        titles.add(ui.label(title, Theme.TextStyle.HEADING_LG, Theme.ON_PAGE_STRONG));
        titleRow.add(titles).expandX().left().bottom();
        titleRow.add(titleActions).right().bottom();
        root.add(titleRow).padTop(Theme.SPACE_2).padBottom(Theme.SPACE_4).row();
        root.add(content).expand().fill().row();
        root.add(line()).height(Theme.BORDER_THIN).padTop(Theme.SPACE_4).row();
        root.add(footer).padTop(Theme.SPACE_2);
        refreshChrome();
    }

    /**
     * Builds a button that runs an action when clicked (and isn't disabled).
     *
     * @param text   the label
     * @param kind   the kind of button
     * @param action the action
     * @return the button
     */
    protected TextButton actionButton(String text, Theme.ButtonKind kind, Runnable action) {
        return onClick(ui.button(text, kind), action);
    }

    /**
     * Makes a button run an action when clicked (and isn't disabled).
     *
     * @param button the button
     * @param action the action
     * @return the same button
     */
    protected static TextButton onClick(TextButton button, Runnable action) {
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
        return button;
    }

    /**
     * Builds a price breakdown ({@code ui-spec.md} component 4): line items, then a total under a
     * navy rule.
     *
     * @param lines the lines
     * @param total the total
     * @return the table
     */
    protected Table priceBreakdown(List<Programs.PriceLine> lines, long total) {
        Table table = new Table();
        table.defaults().growX();
        for (Programs.PriceLine line : lines) {
            Table row = new Table();
            row.add(ui.label(line.label(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).expandX().left();
            row.add(ui.label(line.text() != null ? line.text() : money(line.amount()), Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).right();
            table.add(row).padBottom(Theme.SPACE_1).row();
        }
        table.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_EMPHASIS).padTop(2f).padBottom(Theme.SPACE_2)
            .row();
        Table totalRow = new Table();
        totalRow.add(ui.label("Total", Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
        totalRow.add(ui.label(money(total), Theme.TextStyle.HEADING_SM, Theme.INK)).right();
        table.add(totalRow);
        return table;
    }

    /**
     * Builds a program's state mark: the gold "Open" badge, a navy "Available" pill, or a muted
     * "Locked" pill with a lock.
     *
     * @param state the state
     * @return the mark
     */
    protected Table programStateMark(Programs.State state) {
        Table mark = new Table();
        if (state == Programs.State.OPEN) {
            mark.add(ui.badge(Theme.BadgeKind.ACTIVE, "Open"));
            return mark;
        }
        boolean locked = state == Programs.State.LOCKED;
        Color color = locked ? Theme.INK_MUTED : Theme.NAVY;
        mark.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), locked ? Theme.NAVY_BORDER : Theme.NAVY,
            Theme.BORDER_THIN));
        mark.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        if (locked) {
            Image lock = new Image(ui.icon("lock"), com.badlogic.gdx.utils.Scaling.fit);
            lock.setColor(color);
            mark.add(lock).size(11f).padRight(4f);
        }
        mark.add(ui.label(locked ? "Locked" : "Available", Theme.TextStyle.LABEL, color));
        return mark;
    }

    /**
     * Words an amount of money as the mockups do.
     *
     * @param amount the amount; negative for a deduction
     * @return for example "$ 1,345.0" or "\u2212 $ 336.0"
     */
    protected static String money(long amount) {
        return amount < 0 ? String.format(Locale.ROOT, "\u2212 $ %,.1f", (double) -amount)
            : String.format(Locale.ROOT, "$ %,.1f", (double) amount);
    }

    /**
     * Builds a landing screen's navigation card: an eyebrow, a title, a line of copy, and a
     * primary button.
     *
     * @param eyebrow the eyebrow
     * @param title   the title
     * @param body    the copy
     * @param action  the button's label
     * @param onClick what the button does
     * @return the card
     */
    protected Table navCard(String eyebrow, String title, String body, String action, Runnable onClick) {
        Table card = ui.card();
        card.defaults().left().growX();
        card.add(ui.label(eyebrow, Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(Theme.SPACE_2).row();
        card.add(ui.label(title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(body, Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        card.add(actionButton(action, Theme.ButtonKind.PRIMARY, onClick)).left().fill(false, false);
        return card;
    }

    /**
     * Builds a building panel ({@code ui-spec.md} component 7): the level, the building's cap,
     * its upkeep, and an Upgrade button (or "Upgrading" while it builds) whose confirmation shows
     * the price and what the next level raises the cap to.
     *
     * @param building the building
     * @param name     the building's name, as on screen
     * @param capLabel the cap's label, such as "Open programs"
     * @param capValue the cap's value, such as "2 / 3"
     * @param capNoun  what the cap is called in a sentence, such as "open-program cap"
     * @return the panel
     */
    protected Table buildingPanel(Building building, String name, String capLabel, String capValue, String capNoun) {
        return buildingPanel(building, name, capLabel, capValue, capNoun, List.of(), level -> null);
    }

    /**
     * Builds a building panel with stats of the building's own after the cap (the VAB's rocket
     * classes), and an upgrade confirmation that can name what else the next level unlocks.
     *
     * @param building   the building
     * @param name       its name in the confirmation, such as "Headquarters"
     * @param capLabel   the cap's label, such as "Open programs"
     * @param capValue   the cap's value, such as "2 / 3"
     * @param capNoun    the cap in the confirmation's sentence, such as "open-program cap"
     * @param extraStats more label and value pairs, after the cap
     * @param unlocks    what a level newly unlocks, for the confirmation ("the Medium class"), or {@code null}
     * @return the panel
     */
    protected Table buildingPanel(Building building, String name, String capLabel, String capValue, String capNoun,
                                  List<String[]> extraStats, java.util.function.IntFunction<String> unlocks) {
        PlayerSnapshot state = session.state();
        BuildingSchedule.Levels levels = session.catalog().buildings().of(building);
        int level = state.buildingLevel(building);
        Table panel = ui.card();
        Table stats = new Table();
        List<String[]> values = new java.util.ArrayList<>();
        values.add(new String[] {"Level", level + " of " + levels.maxLevel()});
        values.add(new String[] {capLabel, capValue});
        values.addAll(extraStats);
        values.add(new String[] {"Upkeep / season", money(session.catalog().buildings().maintenance(building, level))});
        for (String[] value : values) {
            Table stat = new Table();
            stat.left();
            stat.defaults().left();
            stat.add(ui.label(value[0].toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f)
                .row();
            stat.add(ui.label(value[1], Theme.TextStyle.HEADING_SM, Theme.INK));
            stats.add(stat).left().padRight(Theme.SPACE_6);
        }
        panel.add(stats).expandX().left();
        Integer upgrading = state.getBuildingUpgradeSeasonsRemaining().get(building);
        if (upgrading != null) {
            panel.add(ui.badge(Theme.BadgeKind.ACTIVE, "Upgrading \u00b7 " + upgrading
                + (upgrading == 1 ? " season left" : " seasons left"))).right();
        } else if (level >= levels.maxLevel()) {
            panel.add(ui.label("Top level", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        } else {
            panel.add(actionButton("Upgrade \u2014 Level " + (level + 1), Theme.ButtonKind.SECONDARY,
                () -> confirmUpgrade(building, name, capNoun, unlocks.apply(level + 1)))).right();
        }
        return panel;
    }

    /**
     * Asks to upgrade a building, showing the price and what the next level raises the cap to.
     *
     * @param building the building
     * @param name     the building's name
     * @param capNoun  what its cap is called in a sentence
     */
    public void confirmUpgrade(Building building, String name, String capNoun) {
        confirmUpgrade(building, name, capNoun, null);
    }

    /**
     * Asks to upgrade a building, naming what else the next level unlocks.
     *
     * @param building the building
     * @param name     its name, such as "the VAB"
     * @param capNoun  the cap in the sentence
     * @param unlocks  what the next level unlocks besides the cap ("the Medium class"), or {@code null}
     */
    public void confirmUpgrade(Building building, String name, String capNoun, String unlocks) {
        BuildingSchedule.Levels levels = session.catalog().buildings().of(building);
        int level = session.state().buildingLevel(building);
        long cost = levels.upgradeCosts().get(level - 1);
        int seasons = levels.upgradeSeasons().get(level - 1);
        String duration = seasons + (seasons == 1 ? " season" : " seasons");
        Table card = dialogCard("Confirm", "Upgrade " + name + " to Level " + (level + 1) + "?");
        card.add(ui.paragraph("Raises the " + capNoun + " from " + levels.caps().get(level - 1) + " to "
            + levels.caps().get(level) + (unlocks == null ? "" : " and unlocks " + unlocks) + ". Takes " + duration + ", and upgrading is final.", Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(priceBreakdown(List.of(new Programs.PriceLine("Upgrade cost", cost, null)), cost))
            .padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Upgrade", Theme.ButtonKind.PRIMARY, () -> submit(new UpgradeBuildingAction(building),
            result -> {
                closeDialog();
                toast("Upgrade started \u2014 " + duration + " left.", Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }

    @Override
    public void render(float delta) {
        countdown.setText(nextTurnText());
        super.render(delta);
    }

    @Override
    protected void stateChanged() {
        refreshChrome();
        rebuild();
    }

    /**
     * Updates the header chips and the footer from the session.
     */
    protected void refreshChrome() {
        PlayerSnapshot state = session.state();
        prestige.setText(String.format(Locale.ROOT, "%,d PRESTIGE", state.getPrestige()));
        gameDate.setText(session.gameInfo().response().date().label().toUpperCase(Locale.ROOT));
        rebuildFooter(state, session.catalog());
    }

    private Table headerBar(List<Crumb> crumbs) {
        Table bar = new Table();
        bar.add(navButton("back", this::goBack)).size(NAV_BUTTON).padRight(Theme.SPACE_2);
        bar.add(navButton("home", this::goHome)).size(NAV_BUTTON).padRight(Theme.SPACE_2);
        bar.add(navButton("settings", () -> notBuiltYet("Settings"))).size(NAV_BUTTON).padRight(Theme.SPACE_2);
        bar.add(new Image(ui.solid(Theme.ON_PAGE))).width(Theme.BORDER_THIN).height(NAV_BUTTON).padRight(Theme.SPACE_2);
        for (int i = 0; i < crumbs.size(); i++) {
            Crumb crumb = crumbs.get(i);
            boolean current = i == crumbs.size() - 1;
            Label label = ui.label(current ? crumb.label().toUpperCase(Locale.ROOT) : crumb.label(),
                Theme.TextStyle.LABEL, current ? Theme.ON_PAGE_STRONG : Theme.ON_PAGE_MUTED);
            if (!current && crumb.action() != null) {
                label.addListener(clicked(() -> crumb.action().accept(this)));
            }
            bar.add(label).padRight(6f);
            if (!current) {
                bar.add(ui.label("›", Theme.TextStyle.LABEL, Theme.ON_PAGE_MUTED)).padRight(6f);
            }
        }
        bar.add().expandX();
        bar.add(chip(ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN), "prestige", Theme.NAVY, prestige))
            .padRight(Theme.SPACE_3);
        bar.add(chip(ui.solid(Theme.NAVY), "game_time", Theme.ON_FILL, gameDate)).padRight(Theme.SPACE_3);
        bar.add(chip(ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN), "clock", Theme.NAVY, countdown));
        return bar;
    }

    private Table navButton(String icon, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.ON_PAGE, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.ON_PAGE_STRONG);
        button.add(glyph).size(16f);
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(action));
        return button;
    }

    private Table chip(com.badlogic.gdx.scenes.scene2d.utils.Drawable background, String icon, Color color, Label text) {
        Table chip = new Table();
        chip.setBackground(background);
        chip.pad(Theme.SPACE_2, 14f, Theme.SPACE_2, 14f);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(color);
        chip.add(glyph).size(14f).padRight(6f);
        chip.add(text);
        return chip;
    }

    private void rebuildFooter(PlayerSnapshot state, ContentCatalog catalog) {
        footer.clearChildren();
        // No funds delta: last season's net change isn't in the state yet (gap G2), so none is invented.
        footer.add(footerStat("funds", String.format(Locale.ROOT, "$ %,d", state.getFunds()),
            () -> notBuiltYet("The Public Affairs Office"))).expandX().left();
        footer.add(footerStat("set_employees", counts(state.getSetSpecialists(), catalog, state, Building.SET_CENTER),
            this::goSetCentre)).expandX();
        footer.add(footerStat("astronauts", counts(state.getAstronauts(), catalog, state, Building.ASTRONAUT_CENTER),
            () -> notBuiltYet("The Astronaut Complex"))).expandX();
        footer.add(footerStat("flight_controllers",
            counts(state.getMissionControlOfficers(), catalog, state, Building.MISSION_CONTROL),
            () -> notBuiltYet("Mission Control"))).expandX();
        footer.add(footerStat("open_programs", state.getOpenedProgramIds().size() + " / "
            + catalog.buildings().capacity(Building.HEADQUARTERS, state.buildingLevel(Building.HEADQUARTERS)),
            this::goHeadquarters)).expandX().right();
    }

    private static String counts(List<? extends Personnel> roster, ContentCatalog catalog, PlayerSnapshot state,
                                 Building building) {
        long busyCount = roster.stream().filter(SetDepartment::isBusy).count();
        return roster.size() + " / " + busyCount + " / "
            + catalog.buildings().capacity(building, state.buildingLevel(building));
    }

    private Table footerStat(String icon, String text, Runnable action) {
        Table stat = new Table();
        stat.setBackground(ui.solid(new Color(1f, 1f, 1f, 0.08f)));
        stat.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.ON_PAGE_STRONG);
        stat.add(glyph).size(16f).padRight(Theme.SPACE_2);
        stat.add(ui.label(text, Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG)).padRight(Theme.SPACE_2);
        Image chevron = new Image(ui.icon("chevron"), Scaling.fit);
        chevron.setColor(new Color(1f, 1f, 1f, 0.6f));
        stat.add(chevron).size(12f);
        stat.setTouchable(Touchable.enabled);
        stat.addListener(clicked(action));
        return stat;
    }

    private Image line() {
        return new Image(ui.solid(Theme.ON_PAGE));
    }

    /**
     * Wraps a list in a vertical scroll pane that the mouse wheel scrolls as soon as the pointer is
     * over it (libGDX only gives a scroll pane wheel focus on a click otherwise).
     *
     * @param list the list
     * @return the scroll pane
     */
    protected ScrollPane scrollPane(Table list) {
        ScrollPane scroll = new ScrollPane(list, new ScrollPane.ScrollPaneStyle());
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setScrollingDisabled(true, false);
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) {
                    stage.setScrollFocus(scroll);
                }
            }
        });
        return scroll;
    }

}
