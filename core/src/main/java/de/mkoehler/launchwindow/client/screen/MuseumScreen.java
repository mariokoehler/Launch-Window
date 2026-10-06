package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Museum;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.rules.WinCondition;

import java.util.List;
import java.util.Locale;

/**
 * The Museum ({@code ui-spec.md} &sect;4.11): the race between the agencies, the player's
 * achievements, completed flights, and the memorial. Tabs in the header, as in the mockup.
 *
 * @author Mario Koehler
 */
public final class MuseumScreen extends ChromeScreen {

    /** The Museum's tabs. */
    public enum Tab {
        /** Every agency's declared goals. */
        RACE("The race"),
        /** Every goal of the pack, held or open. */
        ACHIEVEMENTS("Achievements"),
        /** Completed flights per configuration. */
        FLIGHTS("Flights"),
        /** Astronauts lost. */
        MEMORIAL("Memorial");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** The achievements filter. */
    private enum Filter {
        ALL, DONE, OPEN
    }

    /** The portrait on a memorial plaque. */
    private static final float PORTRAIT_SIZE = 96f;

    private static final String MEMORIAL_NOTE = "Valentin Bondarenko, the youngest of the first Soviet cosmonaut "
        + "group, died in March 1961 after a fire in an oxygen-rich, low-pressure chamber, ten days into a "
        + "fifteen-day endurance test. His death was kept secret until 1986. A fire in a pure-oxygen cabin also "
        + "killed the Apollo 1 crew on the launch pad in 1967.";

    private Tab tab;
    private Filter filter = Filter.ALL;
    private boolean rivalsFailed;

    /**
     * Builds the Museum on a tab, fetching the race's rival summaries.
     *
     * @param game the game, in a session
     * @param tab  the tab to show
     */
    public MuseumScreen(LaunchWindowGame game, Tab tab) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Museum", null)), "Museum", "Goals, flights and the race");
        this.tab = tab;
        rebuild();
        fetchRivals();
    }

    private void fetchRivals() {
        game.gateway().fetchRivals(session.credentials(), result -> {
            if (!isShowing()) {
                return;
            }
            rivalsFailed = !result.ok();
            if (result.ok()) {
                session.keepRivals(result.rivals());
            }
            rebuild();
        });
    }

    /**
     * Switches tabs.
     *
     * @param tab the tab to show
     */
    public void select(Tab tab) {
        this.tab = tab;
        rebuild();
    }

    /**
     * Opens the game result over the Museum, as its race tab's button does.
     */
    public void showResult() {
        new GameResultDialog(this).open();
    }

    @Override
    protected void rebuild() {
        titleActions.clearChildren();
        for (Tab each : Tab.values()) {
            titleActions.add(tabButton(each)).padLeft(Theme.SPACE_2);
        }
        content.clearChildren();
        content.top();
        content.defaults().growX();
        switch (tab) {
            case RACE -> race();
            case ACHIEVEMENTS -> achievements();
            case FLIGHTS -> flights();
            case MEMORIAL -> memorial();
        }
    }

    private Table tabButton(Tab each) {
        boolean active = each == tab;
        Table button = new Table();
        button.setBackground(ui.navTab(active));
        button.pad(Theme.SPACE_2, Theme.SPACE_4, Theme.SPACE_2, Theme.SPACE_4);
        button.add(ui.label(each.label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
            active ? Theme.NAVY : Theme.ON_PAGE_STRONG));
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(() -> select(each)));
        return button;
    }

    // ---- The race

    private void race() {
        GameInfoResponse info = session.gameInfo().response();
        List<String> declared = info.declaredGoalIds();
        if (info.decided()) {
            Table banner = ui.card();
            boolean tied = "TIED".equals(info.resultStatus());
            String names = String.join(" and ", info.winnerAgencies());
            String lead = tied ? "The race is decided: " + names + " share the win, in "
                : "The race is decided: " + names + " won, in ";
            Table notice = ui.notice(Theme.NoticeKind.SUCCESS, lead + Museum.date((int) info.resultTurn()) + ".",
                "Play goes on: goals, flights and prestige still count, and the table below keeps filling in.");
            banner.add(notice).expandX().fillX().padRight(Theme.SPACE_5);
            banner.add(actionButton("Show the result", Theme.ButtonKind.PRIMARY, this::showResult));
            content.add(banner).padBottom(Theme.SPACE_4).row();
        }

        Table columns = new Table();
        columns.top();
        Table raceCard = ui.card();
        raceCard.top();
        raceCard.defaults().growX();
        List<RivalSummary> rivals = session.rivals();
        Table head = new Table();
        String count = rivals == null ? "" : " · " + rivals.size() + (rivals.size() == 1 ? " agency" : " agencies");
        head.add(ui.label(("The race" + count).toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY))
            .expandX().left();
        head.add(ui.label("Dates are the turn each goal was done", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        raceCard.add(head).padBottom(Theme.SPACE_3).row();
        if (rivals == null) {
            raceCard.add(ui.paragraph(rivalsFailed ? "Couldn't fetch the race. Open the Museum again to retry."
                : "Fetching the race…", Theme.TextStyle.BODY, rivalsFailed ? Theme.DANGER : Theme.INK_MUTED))
                .padBottom(Theme.SPACE_3).row();
        } else {
            raceCard.add(RaceTable.build(ui, Museum.race(rivals, declared), declared, true)).padBottom(Theme.SPACE_3)
                .row();
        }
        raceCard.add(ui.paragraph("First to finish all " + declared.size() + " wins. If several agencies finish at "
            + "the same turn, a medal table over the " + declared.size() + " goals decides: the most first places, "
            + "then the most second places, and so on. A goal credited through another counts from the turn that "
            + "goal was reached.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        columns.add(raceCard).expandX().fillX().top().padRight(Theme.SPACE_4);
        columns.add(declaredCard(declared)).width(460f).top();
        content.add(scrollPane(columns)).expand().fill();
    }

    private Table declaredCard(List<String> declared) {
        PlayerSnapshot state = session.state();
        ContentCatalog catalog = session.catalog();
        java.util.Map<String, Integer> times = WinCondition.goalTimes(state, declared, catalog);
        Table card = ui.card();
        card.top();
        card.defaults().growX();
        Table head = new Table();
        head.add(ui.label("THIS GAME IS WON BY", Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        head.add(ui.label("Set when the game was created", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(head).padBottom(Theme.SPACE_2).row();
        for (int i = 0; i < declared.size(); i++) {
            String goalId = declared.get(i);
            Integer turn = times.get(goalId);
            Table row = new Table();
            row.add(RaceTable.patch(ui, goalId, turn != null, RaceTable.PATCH_SIZE)).size(RaceTable.PATCH_SIZE)
                .padRight(14f);
            Table text = new Table();
            text.defaults().left().growX();
            text.add(ui.label(ContentNames.of(goalId), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            String about = ContentNames.description(goalId);
            if (about != null) {
                text.add(ui.paragraph(about, Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(2f).row();
            }
            String credited = creditedBy(goalId, catalog);
            if (!credited.isEmpty()) {
                text.add(ui.paragraph(credited, Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
            }
            text.add(ui.label(turn == null ? "Not yet" : "Done " + Museum.date(turn), Theme.TextStyle.CAPTION,
                turn == null ? Theme.INK_MUTED : Theme.SUCCESS));
            row.add(text).expandX().fillX();
            card.add(row).padTop(Theme.SPACE_3).padBottom(Theme.SPACE_3).row();
            if (i < declared.size() - 1) {
                card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
            }
        }
        return card;
    }

    /** Names the goals whose cascade credits this one directly. */
    private static String creditedBy(String goalId, ContentCatalog catalog) {
        List<String> by = catalog.goals().stream().filter(goal -> goal.cancelsGoalIds().contains(goalId))
            .map(Goal::id).map(ContentNames::of).toList();
        return by.isEmpty() ? "" : "Also credited by " + String.join(", ", by) + ".";
    }

    // ---- Achievements

    private void achievements() {
        PlayerSnapshot state = session.state();
        ContentCatalog catalog = session.catalog();
        Museum.AchievementSummary summary = Museum.summary(state, catalog);
        Table strip = ui.card();
        strip.pad(Theme.BORDER_EMPHASIS);
        stat(strip, "Goals done", summary.done() + " of " + summary.total(), true);
        stat(strip, "First in the game", String.valueOf(summary.firsts()), true);
        stat(strip, "Credited through the cascade", String.valueOf(summary.credited()), true);
        stat(strip, "Prestige from goals", String.format(Locale.ROOT, "%,d", summary.prestige()), false);
        content.add(strip).padBottom(Theme.SPACE_4).row();

        List<Museum.Achievement> all = Museum.achievements(state, catalog);
        long done = all.stream().filter(Museum.Achievement::done).count();
        Table bar = new Table();
        Table chips = new Table();
        filterChip(chips, Filter.ALL, "All · " + all.size());
        filterChip(chips, Filter.DONE, "Done · " + done);
        filterChip(chips, Filter.OPEN, "Open · " + (all.size() - done));
        bar.add(chips).left();
        bar.add().expandX();
        bar.add(ui.label("A goal pays its first-time prestige to the first agency in the game to reach it, and less "
            + "to everyone after.", Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG));
        content.add(bar).padBottom(Theme.SPACE_4).row();

        Table card = ui.card();
        card.top();
        List<Museum.Achievement> rows = all.stream().filter(row -> filter == Filter.ALL
            || (filter == Filter.DONE) == row.done()).toList();
        Table left = new Table();
        Table right = new Table();
        left.top();
        right.top();
        left.defaults().growX();
        right.defaults().growX();
        int half = (rows.size() + 1) / 2;
        for (int i = 0; i < rows.size(); i++) {
            Table column = i < half ? left : right;
            column.add(achievement(rows.get(i))).row();
            column.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        if (rows.isEmpty()) {
            card.add(ui.paragraph(filter == Filter.DONE ? "No goals yet." : "Every goal is done.",
                Theme.TextStyle.BODY, Theme.INK_MUTED)).expandX().left();
        } else {
            card.add(left).expandX().fillX().top().uniformX().padRight(32f);
            card.add(right).expandX().fillX().top().uniformX();
        }
        content.add(scrollPane(card)).expand().fill();
    }

    private void stat(Table strip, String label, String value, boolean divider) {
        Table stat = new Table();
        stat.pad(12f, 16f, 12f, 16f);
        stat.left();
        stat.defaults().left();
        stat.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f)
            .row();
        stat.add(ui.label(value, Theme.TextStyle.HEADING_SM, Theme.INK));
        strip.add(stat).expandX().fillX().uniformX().left();
        if (divider) {
            strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
        }
    }

    private void filterChip(Table chips, Filter option, String text) {
        boolean active = option == filter;
        Table chip = new Table();
        chip.setBackground(active ? ui.solid(Theme.SURFACE_CARD)
            : ui.rect(new Color(0f, 0f, 0f, 0f), Theme.ON_PAGE, Theme.BORDER_THIN));
        chip.pad(5f, 12f, 5f, 12f);
        chip.add(ui.label(text, Theme.TextStyle.LABEL, active ? Theme.NAVY : Theme.ON_PAGE_STRONG));
        chip.setTouchable(Touchable.enabled);
        chip.addListener(clicked(() -> {
            filter = option;
            rebuild();
        }));
        chips.add(chip).padRight(6f);
    }

    private Table achievement(Museum.Achievement row) {
        Table line = new Table();
        line.pad(10f, 0f, 10f, 0f);
        line.add(RaceTable.patch(ui, row.goalId(), row.done(), RaceTable.PATCH_SIZE)).size(RaceTable.PATCH_SIZE)
            .padRight(14f);
        Table text = new Table();
        text.left();
        text.defaults().left();
        Table name = new Table();
        name.add(ui.label(row.name(), Theme.TextStyle.BODY_STRONG, Theme.INK));
        if (row.first()) {
            name.add(RaceTable.firstTag(ui, "First in the game")).padLeft(Theme.SPACE_2);
        }
        text.add(name).row();
        text.add(ui.label(row.how(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        line.add(text).expandX().fillX().left();
        Table right = new Table();
        right.defaults().right();
        right.add(ui.label(row.prestige(), row.done() ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.CAPTION,
            row.done() ? Theme.SUCCESS : Theme.INK_MUTED)).row();
        if (!row.when().isEmpty()) {
            right.add(ui.label(row.when(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        line.add(right).right();
        return line;
    }

    // ---- Flights

    private void flights() {
        List<Museum.Flight> flights = Museum.flights(session.state(), session.catalog());
        int total = flights.stream().mapToInt(Museum.Flight::count).sum();
        Table head = new Table();
        head.add(ui.label(flights.isEmpty() ? "No completed flights yet" : total + (total == 1 ? " completed flight"
            : " completed flights") + ", in " + flights.size() + (flights.size() == 1 ? " configuration"
            : " configurations"), Theme.TextStyle.HEADING_SM, Theme.ON_PAGE_STRONG)).expandX().left();
        head.add(ui.label("Only flights that completed count. A scrub, a cancel or a failure that ended a mission "
            + "doesn't.", Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG));
        content.add(head).padBottom(Theme.SPACE_4).row();
        Table grid = new Table();
        grid.top().left();
        for (int i = 0; i < flights.size(); i++) {
            grid.add(flight(flights.get(i))).width(352f).top().padRight(i % 5 == 4 ? 0f : 16f).padBottom(16f);
            if (i % 5 == 4) {
                grid.row();
            }
        }
        content.add(scrollPane(grid)).expand().fill();
    }

    private Table flight(Museum.Flight flight) {
        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.defaults().growX().left();
        Drawable art = ui.croppedPicture("configurations/" + flight.configurationId() + ".jpg", 480f / 309f);
        card.add(new Image(art)).height(226f).row();
        Table text = new Table();
        text.pad(10f, 16f, 16f, 16f);
        text.left();
        text.defaults().left();
        text.add(ui.label(flight.program().toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).row();
        text.add(ui.label(flight.name(), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(6f).row();
        Table count = new Table();
        count.add(ui.label(String.valueOf(flight.count()), Theme.TextStyle.HEADING_LG, Theme.NAVY)).padRight(6f);
        count.add(ui.label(flight.count() == 1 ? "completed flight" : "completed flights", Theme.TextStyle.BODY_SM,
            Theme.INK_MUTED)).bottom().padBottom(3f);
        text.add(count).row();
        text.add(ui.label(flight.crewed() ? "Crewed" : "Uncrewed", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(text).row();
        return card;
    }

    // ---- Memorial

    private void memorial() {
        Table head = new Table();
        head.add(ui.label("In memory", Theme.TextStyle.HEADING_SM, Theme.ON_PAGE_STRONG)).padRight(Theme.SPACE_2);
        Table slot = new Table();
        slot.setBackground(ui.solid(Theme.SURFACE_CARD));
        slot.pad(2f);
        slot.add(new HistoryNote(ui, MEMORIAL_NOTE)).size(HistoryNote.ICON_SIZE);
        head.add(slot);
        head.add().expandX();
        content.add(head).padBottom(Theme.SPACE_4).row();
        List<Museum.Plaque> plaques = Museum.memorial(session.state(), session.catalog());
        Table list = new Table();
        list.top();
        if (plaques.isEmpty()) {
            list.add(ui.label("No one has been lost.", Theme.TextStyle.HEADING_SM, Theme.ON_PAGE_STRONG)).padTop(60f)
                .padBottom(6f).row();
            list.add(ui.label("Astronauts lost on a mission or in an accident, and animals that flew and didn't come "
                + "home, are remembered here.",
                Theme.TextStyle.BODY_SM, Theme.ON_PAGE_STRONG)).row();
        }
        for (Museum.Plaque plaque : plaques) {
            list.add(plaque(plaque)).width(620f).padBottom(Theme.SPACE_4).row();
        }
        content.add(scrollPane(list)).expand().fill();
    }

    private Table plaque(Museum.Plaque plaque) {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(28f, 32f, 28f, 32f);
        TextureRegionDrawable portrait = plaque.portraitId() == null ? null
            : ui.picture("portraits/" + plaque.portraitId() + ".jpg");
        if (portrait != null) {
            card.add(new Image(portrait, Scaling.fill)).size(PORTRAIT_SIZE).padBottom(14f).row();
        }
        card.add(ui.label(plaque.kind().toUpperCase(java.util.Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY))
            .padBottom(8f).row();
        card.add(ui.label(plaque.name(), Theme.TextStyle.HEADING_LG, Theme.INK)).padBottom(8f).row();
        card.add(new Image(ui.solid(Theme.NAVY))).size(64f, Theme.BORDER_EMPHASIS).padBottom(8f).row();
        card.add(ui.paragraph(plaque.line(), Theme.TextStyle.BODY, Theme.INK)).growX().padBottom(8f).row();
        card.add(ui.paragraph(plaque.detail(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).growX().row();
        for (com.badlogic.gdx.scenes.scene2d.Actor actor : card.getChildren()) {
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.Label label) {
                label.setAlignment(Align.center);
            }
        }
        return card;
    }
}
