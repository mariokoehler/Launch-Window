package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.content.SpaceRaceNews;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.OutcomeBar;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The turn report ({@code ui-spec.md} &sect;4.4): "what happened overnight". An at-a-glance strip,
 * then three columns: missions flown (each with its steps' outcomes and a replay), goals and
 * buildings; R&amp;D and news; people and the budget. Built from one {@link TurnReport}.
 *
 * @author Mario Koehler
 */
public final class TurnReportScreen extends ChromeScreen {

    private final TurnReport report;
    private final TextButton previous;
    private final TextButton next;

    /**
     * Builds the screen for a report.
     *
     * @param game   the game, in a session
     * @param report the report
     */
    public TurnReportScreen(LaunchWindowGame game, TurnReport report) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Turn report", null)), "Turn report · "
            + GameDate.ofTurn(report.gameTurn()).label() + " → " + GameDate.ofTurn(report.gameTurn() + 1).label(),
            "What happened overnight");
        this.report = report;
        previous = onClick(ui.pageButton("Previous turn"), () -> openReportAt(report.gameTurn() - 1));
        next = onClick(ui.pageButton("Next turn"), () -> openReportAt(report.gameTurn() + 1));
        titleActions.add(previous).padRight(Theme.SPACE_2);
        titleActions.add(next).padRight(Theme.SPACE_4);
        titleActions.add(actionButton("Continue to Space Complex", Theme.ButtonKind.PRIMARY, this::goHome));
        rebuild();
    }

    @Override
    protected void rebuild() {
        ContentCatalog catalog = session.catalog();
        previous.setDisabled(!session.mayHaveReport(report.gameTurn() - 1));
        next.setDisabled(!session.mayHaveReport(report.gameTurn() + 1));
        content.clearChildren();
        content.top();
        content.defaults().growX();
        content.add(glance(catalog)).padBottom(Theme.SPACE_4).row();

        Table columns = new Table();
        columns.top();
        Table first = column();
        first.add(missions(catalog)).row();
        first.add(goals()).row();
        first.add(buildings()).row();
        Table second = column();
        second.add(rnd(catalog)).row();
        second.add(news(catalog)).row();
        Table third = column();
        third.add(people(catalog)).row();
        third.add(budget()).row();
        columns.add(first).expandX().fillX().uniformX().top().padRight(Theme.SPACE_4);
        columns.add(second).expandX().fillX().uniformX().top().padRight(Theme.SPACE_4);
        columns.add(third).expandX().fillX().uniformX().top();
        content.add(scrollPane(columns)).expand().fill();
    }

    private Table column() {
        Table column = new Table();
        column.top();
        column.defaults().growX().padBottom(Theme.SPACE_4);
        return column;
    }

    private Table glance(ContentCatalog catalog) {
        Table strip = ui.card();
        strip.pad(Theme.BORDER_EMPHASIS);
        List<TurnReportView.Glance> stats = TurnReportView.glance(report, catalog);
        for (int i = 0; i < stats.size(); i++) {
            TurnReportView.Glance stat = stats.get(i);
            Table cell = new Table();
            cell.left();
            cell.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
            cell.defaults().left();
            cell.add(ui.label(stat.label().toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padBottom(2f).row();
            cell.add(ui.label(stat.value(), Theme.TextStyle.HEADING_SM, tone(stat.tone()))).row();
            cell.add(ui.label(stat.note(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            strip.add(cell).expandX().fillX().uniformX().top();
            if (i < stats.size() - 1) {
                strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
            }
        }
        return strip;
    }

    private static Color tone(int tone) {
        return tone > 0 ? Theme.SUCCESS : tone < 0 ? Theme.DANGER : Theme.INK;
    }

    private Table section(String title, String note) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label(title.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        if (note != null) {
            head.add(ui.label(note, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        }
        card.add(head).padBottom(Theme.SPACE_3).row();
        return card;
    }

    private Table missions(ContentCatalog catalog) {
        Table card = section("Missions flown", "Each square is one step");
        List<TurnReportView.MissionLine> lines = TurnReportView.missions(report, catalog);
        if (lines.isEmpty()) {
            card.add(ui.paragraph("No missions flew this season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (TurnReportView.MissionLine line : lines) {
            Table row = new Table();
            Drawable art = ui.croppedPicture("configurations/" + line.flown().missionConfigurationId() + ".jpg", 1.6f);
            row.add(new Image(art == null ? ui.solid(Theme.NAVY) : art)).size(80f, 50f).top().padRight(Theme.SPACE_3);
            Table text = new Table();
            text.left();
            text.defaults().left().growX();
            text.add(ui.label(line.name(), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(2f).row();
            text.add(ui.paragraph(line.result(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(4f).row();
            Table dots = new Table();
            dots.left();
            for (MissionStepOutcome outcome : line.outcomes()) {
                dots.add(new Image(ui.solid(OutcomeBar.COLORS[outcome.ordinal()]))).size(10f).padRight(3f);
            }
            text.add(dots).padBottom(Theme.SPACE_2).row();
            text.add(actionButton("Watch replay", Theme.ButtonKind.SECONDARY,
                () -> game.setScreen(new ReplayScreen(game, report, line.flown().missionInstanceId()))))
                .left().fill(false, false);
            row.add(text).expandX().fillX().padRight(Theme.SPACE_3);
            row.add(ui.label(TurnReportView.signed(line.prestige()), Theme.TextStyle.HEADING_SM,
                tone(Long.signum(line.prestige())))).top();
            card.add(row).padBottom(Theme.SPACE_3).row();
        }
        return card;
    }

    private Table goals() {
        List<TurnReportView.GoalLine> lines = TurnReportView.goals(report);
        Table card = section("Goals", lines.isEmpty() ? null : "Reaching a goal also credits every goal it cancels");
        if (lines.isEmpty()) {
            card.add(ui.paragraph("No new goals this season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (TurnReportView.GoalLine line : lines) {
            Table row = new Table();
            Table left = new Table();
            left.left();
            left.defaults().left();
            Table chips = new Table();
            chips.add(chip(ContentNames.of(line.goalId()), Theme.SUCCESS, new Color(Theme.SURFACE_CARD)
                .lerp(Theme.SUCCESS, 0.08f)));
            if (line.first()) {
                chips.add(chip("First in the game!", Theme.GOLD_TEXT, new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD,
                    0.15f))).padLeft(4f);
            }
            left.add(chips).padBottom(2f).row();
            left.add(ui.label(line.how(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            row.add(left).expandX().fillX().left();
            row.add(ui.label(TurnReportView.signed(line.prestige()), Theme.TextStyle.BODY_STRONG, Theme.SUCCESS));
            card.add(row).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table chip(String text, Color color, Color fill) {
        Table chip = new Table();
        chip.setBackground(ui.rect(fill, color, Theme.BORDER_THIN));
        chip.pad(2f, 6f, 2f, 6f);
        chip.add(ui.label(text, Theme.TextStyle.LABEL, color));
        return chip;
    }

    private Table buildings() {
        Table card = section("Buildings", null);
        List<TurnReportView.Line> lines = TurnReportView.buildings(report);
        if (lines.isEmpty()) {
            card.add(ui.paragraph("No upgrades finished this season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (TurnReportView.Line line : lines) {
            card.add(line(line.title(), line.text(), Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table line(String text, String right, Color rightColor) {
        Table row = new Table();
        row.add(ui.paragraph(text, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX().left().padRight(Theme.SPACE_2);
        row.add(ui.label(right, Theme.TextStyle.CAPTION, rightColor)).right().top();
        return row;
    }

    private Table rnd(ContentCatalog catalog) {
        Table card = section("Research & development", null);
        List<TurnReportView.RndLine> lines = TurnReportView.rnd(report);
        if (lines.isEmpty()) {
            card.add(ui.paragraph("No reliability changed this season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
                .row();
        }
        for (TurnReportView.RndLine line : lines) {
            Optional<MissionComponent> component = catalog.missionComponent(line.componentId());
            Table head = new Table();
            head.add(ui.label(ContentNames.of(line.componentId()), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX()
                .left();
            double delta = line.after() - line.before();
            head.add(ui.label(String.format(Locale.ROOT, "%s %.1f", delta < 0 ? "−" : "+", Math.abs(delta)),
                Theme.TextStyle.BODY_STRONG, delta < 0 ? Theme.DANGER : Theme.SUCCESS));
            card.add(head).padBottom(4f).row();
            if (component.isPresent()) {
                card.add(new ReliabilityBar(ui, line.after(), component.get().maxRdReliability(),
                    component.get().maxReliability(), 0)).height(12f).padBottom(4f).row();
            }
            card.add(ui.paragraph(String.format(Locale.ROOT, "%.1f → %.1f. %s", line.before(), line.after(),
                line.who()), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        }
        for (TurnReportView.Line capped : TurnReportView.capped(report)) {
            card.add(ui.notice(Theme.NoticeKind.SUCCESS, capped.title(), capped.text())).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table news(ContentCatalog catalog) {
        Table card = section("News", "This season");
        TurnReportView.realNews(report, SpaceRaceNews.standard(), session.state().getArchetype()).ifPresent(story -> {
            Table real = new Table();
            real.setBackground(ui.cardAccent(Theme.NAVY));
            real.pad(Theme.SPACE_3, Theme.SPACE_3 + Theme.BORDER_ACCENT, Theme.SPACE_3, Theme.SPACE_3);
            real.left();
            real.defaults().left().growX();
            real.add(ui.label(("On the real timeline · " + GameDate.ofTurn(report.gameTurn()).label())
                .toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(4f).row();
            real.add(ui.paragraph(story.title(), Theme.TextStyle.BODY_STRONG, Theme.INK)).padBottom(2f).row();
            real.add(ui.paragraph(story.text(), Theme.TextStyle.BODY_SM, Theme.INK));
            if (!story.tag().isEmpty()) {
                real.row();
                real.add(ui.paragraph("At home: " + story.tag(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padTop(4f);
            }
            card.add(real).padBottom(Theme.SPACE_4).row();
        });
        List<TurnReportView.Line> lines = TurnReportView.news(report, catalog);
        if (lines.isEmpty()) {
            card.add(ui.paragraph("A quiet season: no random events.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (TurnReportView.Line line : lines) {
            Table row = new Table();
            Image arrow = new Image(ui.icon(line.tone() > 0 ? "sort_asc" : "sort_desc"), Scaling.fit);
            arrow.setColor(line.tone() > 0 ? Theme.SUCCESS : Theme.DANGER);
            row.add(arrow).size(14f).top().padRight(Theme.SPACE_2);
            Table text = new Table();
            text.left();
            text.defaults().left().growX();
            text.add(ui.label(line.title(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            text.add(ui.paragraph(line.text(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            row.add(text).expandX().fillX();
            card.add(row).padBottom(Theme.SPACE_3).row();
        }
        return card;
    }

    private Table people(ContentCatalog catalog) {
        Table card = section("People", null);
        // Morale warnings are about now, so only the latest report shows them.
        boolean latest = report.gameTurn() >= session.latestResolvedTurn();
        List<TurnReportView.Line> lines = TurnReportView.people(report, latest ? session.state() : null, catalog);
        if (lines.isEmpty()) {
            card.add(ui.paragraph("No changes this season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (TurnReportView.Line line : lines) {
            Color color = line.tone() > 0 ? Theme.SUCCESS : line.tone() < 0 ? Theme.DANGER : Theme.INK_MUTED;
            card.add(line(line.title(), line.tag().toUpperCase(Locale.ROOT), color)).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table budget() {
        Table card = section("Budget", "This season");
        for (TurnReportView.BudgetLine line : TurnReportView.budget(report)) {
            Table row = new Table();
            row.add(ui.label(line.label(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            row.add(ui.label(TurnReportView.signedMoney(line.amount()), Theme.TextStyle.BODY_SM,
                line.amount() > 0 ? Theme.SUCCESS : Theme.INK));
            card.add(row).padBottom(4f).row();
        }
        card.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_EMPHASIS).padTop(2f).padBottom(6f).row();
        Table net = new Table();
        net.add(ui.label("Net", Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
        long amount = TurnReportView.net(report);
        net.add(ui.label(TurnReportView.signedMoney(amount), Theme.TextStyle.HEADING_SM, amount < 0 ? Theme.DANGER
            : Theme.SUCCESS));
        card.add(net).padBottom(4f).row();
        Table now = new Table();
        now.add(ui.label("Funds now", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        Label funds = ui.label(money(report.fundsAfter()), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        funds.setAlignment(Align.right);
        now.add(funds);
        card.add(now).padBottom(Theme.SPACE_3).row();
        TurnReportView.review(report).ifPresent(review -> card.add(ui.notice(Theme.NoticeKind.INFO, review.title(),
            review.text())).row());
        return card;
    }
}
