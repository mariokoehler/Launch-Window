package de.mkoehler.launchwindow.client.screen;

import de.mkoehler.launchwindow.rules.PrDuty;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import de.mkoehler.launchwindow.actions.ConvertPrestigeAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Outlook;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.NewsEntry;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.PrestigeConversion;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Public Affairs Office ({@code ui-spec.md} &sect;4.10): funds and running costs; prestige, the
 * next funding review and the review table; active effects and the news log. Convert prestige
 * to funds while in debt. Every number comes from {@code core}'s rules ({@link BudgetProgress},
 * {@link PrestigeConversion}).
 *
 * <p>Reviews and conversions are priced with the game's own budget schedule, which the game info
 * carries ({@link de.mkoehler.launchwindow.net.messages.GameInfoResponse#budgetSchedule()}).</p>
 *
 * @author Mario Koehler
 */
public final class PublicAffairsScreen extends ChromeScreen {


    private TurnReport lastReport;

    /** The game's budget schedule, from the game info. */
    /** A runway this short or shorter is shown in red. */
    private static final int RUNWAY_WARNING_SEASONS = 4;

    private BudgetSchedule schedule() {
        return session.gameInfo().response().budgetSchedule();
    }

    /**
     * Builds the screen from the session, fetching the latest turn report for last season's change.
     *
     * @param game the game, in a session
     */
    public PublicAffairsScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Public Affairs Office", null)), "Public Affairs Office",
            "Budget and prestige");
        lastReport = session.report(session.latestResolvedTurn());
        rebuild();
        if (lastReport == null && session.latestResolvedTurn() >= 1) {
            game.gateway().fetchTurnReport(session.credentials(), -1, result -> {
                if (isShowing() && result.report() != null) {
                    session.keepReport(result.report());
                    lastReport = result.report();
                    rebuild();
                }
            });
        }
    }

    @Override
    protected void goPublicAffairs() {
        // Already here.
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.PUBLIC_AFFAIRS;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        Table columns = new Table();
        columns.top();
        columns.add(fundsColumn()).width(560f).top().fillY().padRight(Theme.SPACE_4);
        columns.add(reviewColumn()).expandX().fillX().top().fillY().padRight(Theme.SPACE_4);
        columns.add(newsColumn()).width(500f).top().fillY();
        content.add(columns).expand().fill();
    }

    // ---- Column 1: funds and running costs

    private Table fundsColumn() {
        PlayerSnapshot state = session.state();
        BudgetProgress.CostDetail costs = BudgetProgress.costDetail(state, session.catalog());
        long funding = state.getFundingRatePerSeason();
        long net = funding - costs.total();
        boolean debt = state.getFunds() < 0;

        Table column = new Table();
        column.top();
        column.defaults().growX();
        Table card = ui.card();
        card.defaults().growX().left();
        if (debt) {
            card.add(ui.notice(Theme.NoticeKind.DANGER, "In debt.", "Anything that costs money is refused until "
                + "funds are above zero: opening programs and rocket programs, launches and upgrades. Salaries, "
                + "upkeep and funding go on.")).padBottom(Theme.SPACE_3).row();
        }
        Table head = new Table();
        Table big = new Table();
        big.left();
        big.defaults().left();
        big.add(ui.label("FUNDS", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        if (debt) {
            // A negative value is a solid danger tag with white text (ui-spec.md §4.10, decided).
            Table tag = new Table();
            tag.setBackground(ui.solid(Theme.DANGER));
            tag.pad(0f, 6f, 0f, 6f);
            tag.add(ui.label(money(state.getFunds()), Theme.TextStyle.HEADING_LG, Theme.ON_FILL));
            big.add(tag).row();
        } else {
            big.add(ui.label(money(state.getFunds()), Theme.TextStyle.HEADING_LG, Theme.NAVY)).row();
        }
        String last = lastSeasonLine();
        if (last != null) {
            big.add(ui.label(last, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(2f);
        }
        head.add(big).expandX().left();
        if (debt) {
            head.add(actionButton("Convert prestige", Theme.ButtonKind.PRIMARY, this::openConvert)).bottom();
        }
        card.add(head).padBottom(Theme.SPACE_3).row();
        card.add(netLines(funding, costs.total(), net)).row();
        // How long the funds last (the user's request, 2026-10-10); in debt, the notice above says it.
        if (!debt) {
            java.util.OptionalInt runway = BudgetProgress.runway(state, session.catalog(), schedule(), 0);
            Label line = ui.paragraph(Outlook.runwayLine(state, session.catalog(), schedule(), 0, 0,
                session.openTurn()), Theme.TextStyle.CAPTION, runway.isPresent() && runway.getAsInt()
                <= RUNWAY_WARNING_SEASONS ? Theme.DANGER : Theme.INK_MUTED);
            ui.tooltip(line, "How long the funds last", "Each season pays today's running costs and today's "
                + "funding; the next review pays what today's prestige earns, from then on. Launches, hiring, "
                + "prestige earned and later reviews aren't predicted.");
            card.add(line).padTop(Theme.SPACE_2).row();
        }
        column.add(card).padBottom(Theme.SPACE_4).row();

        Table costCard = ui.card();
        costCard.top();
        costCard.defaults().growX().left();
        Table title = new Table();
        title.add(ui.label("RUNNING COSTS", Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        title.add(ui.label("per season, paid at each turn", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        costCard.add(title).padBottom(Theme.SPACE_2).row();
        for (BudgetProgress.CostGroup group : costs.groups()) {
            costCard.add(ui.label(group.title().toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padTop(Theme.SPACE_2).padBottom(2f).row();
            for (BudgetProgress.CostLine line : group.lines()) {
                costCard.add(row(line.label(), money(line.amount()), Theme.INK_MUTED, Theme.TextStyle.BODY_SM)).row();
            }
        }
        if (costs.multiplierSeasons() > 0) {
            costCard.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).padTop(4f).row();
            costCard.add(row("All running costs", money(costs.base()), Theme.INK, Theme.TextStyle.BODY_SM)).row();
            costCard.add(row(String.format(Locale.ROOT, "× %.2f %s, %d more %s", costs.multiplier(),
                costs.multiplier() < 1 ? "Reduced expenditures" : "Cost increase", costs.multiplierSeasons(),
                costs.multiplierSeasons() == 1 ? "season" : "seasons"),
                (costs.saving() >= 0 ? "− " : "+ ") + money(Math.abs(costs.saving())),
                costs.saving() >= 0 ? Theme.SUCCESS : Theme.DANGER, Theme.TextStyle.BODY_SM)).row();
        }
        costCard.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_EMPHASIS).padTop(Theme.SPACE_2).row();
        costCard.add(row("Total", money(costs.total()), Theme.INK, Theme.TextStyle.HEADING_SM)).padTop(4f).row();
        column.add(scrollPane(costCard)).expand().fill();
        return column;
    }

    private Table netLines(long funding, long running, long net) {
        Table lines = new Table();
        lines.defaults().growX();
        lines.add(row("Funding" + reviewSource(), "+ " + money(funding), Theme.SUCCESS, Theme.TextStyle.BODY_SM))
            .row();
        lines.add(row("Running costs (below)", "− " + money(running), Theme.INK_MUTED, Theme.TextStyle.BODY_SM))
            .row();
        lines.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_EMPHASIS).padTop(4f).row();
        lines.add(row("Net per season", TurnReportView.signedMoney(net), net >= 0 ? Theme.SUCCESS : Theme.DANGER,
            Theme.TextStyle.HEADING_SM)).padTop(4f).row();
        return lines;
    }

    /** Names what set today's funding: the last review, or the starting rate. */
    private String reviewSource() {
        int last = lastReviewNumber();
        return last == 0 ? ", the starting rate" : ", set by review #" + last;
    }

    private int lastReviewNumber() {
        return BudgetProgress.forecast(session.state(), schedule()).reviewNumber() - 1;
    }

    private String lastSeasonLine() {
        if (lastReport == null) {
            return null;
        }
        long change = lastReport.fundsAfter() - lastReport.fundsBefore();
        return "Last season: " + TurnReportView.signedMoney(change) + ", everything included.";
    }

    private Table row(String label, String value, Color valueColor, Theme.TextStyle style) {
        Table row = new Table();
        row.add(ui.label(label, Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).expandX().left().padRight(Theme.SPACE_4);
        row.add(ui.label(value, style, valueColor)).right();
        return row;
    }

    // ---- Column 2: prestige and funding reviews

    private Table reviewColumn() {
        PlayerSnapshot state = session.state();
        BudgetProgress.ReviewForecast forecast = BudgetProgress.forecast(state, schedule());
        Table column = new Table();
        column.top();
        column.defaults().growX();
        Table card = ui.card();
        card.defaults().growX().left();
        Table head = new Table();
        Table left = new Table();
        left.left();
        left.defaults().left();
        left.add(ui.label("PRESTIGE", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        left.add(ui.label(String.format(Locale.ROOT, "%,d", state.getPrestige()), Theme.TextStyle.HEADING_LG,
            Theme.NAVY)).row();
        long pr = PrDuty.perSeason(state);
        if (pr > 0) {
            left.add(ui.label("PR duty: +" + pr + " / season", Theme.TextStyle.CAPTION, Theme.SUCCESS));
        }
        head.add(left).expandX().left();
        Table right = new Table();
        right.right();
        right.defaults().right();
        right.add(ui.label("NEXT FUNDING REVIEW", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        right.add(ui.label("#" + forecast.reviewNumber() + " in " + forecast.seasonsUntil()
            + (forecast.seasonsUntil() == 1 ? " season" : " seasons"), Theme.TextStyle.HEADING_SM, Theme.INK)).row();
        String date = GameDate.ofTurn(session.gameInfo().response().gameTurn() + forecast.seasonsUntil() - 1).label();
        right.add(ui.label("in " + date + ". Reviews come every " + BudgetProgress.SEASONS_PER_REVIEW + " seasons.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        head.add(right).right();
        card.add(head).padBottom(Theme.SPACE_4).row();

        card.add(ui.label(("Review #" + forecast.reviewNumber() + ": what prestige earns").toUpperCase(Locale.ROOT),
            Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        card.add(new ReviewScale(forecast, state.getPrestige())).height(30f).padBottom(4f).row();
        Table ends = new Table();
        ends.add(ui.label(String.format(Locale.ROOT, "%,d prestige or less: %s", Math.max(0, forecast.minPrestige()),
            money(forecast.minFunding())), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        ends.add(ui.label(String.format(Locale.ROOT, "%,d or more: %s", forecast.maxPrestige(),
            money(forecast.maxFunding())), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        card.add(ends).padBottom(Theme.SPACE_2).row();
        Table legend = new Table();
        legend.add(legendItem(Theme.NAVY, String.format(Locale.ROOT, "You, %,d", state.getPrestige())))
            .padRight(Theme.SPACE_4);
        if (forecast.keepPrestige() >= 0) {
            legend.add(legendItem(Theme.GOLD, String.format(Locale.ROOT, "Keeps today's %s: %,d",
                money(forecast.currentRate()), forecast.keepPrestige()))).padRight(Theme.SPACE_4);
        }
        legend.add(ui.label("A straight line in between, rounded up.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .expandX().left();
        card.add(legend).padBottom(Theme.SPACE_3).row();

        long diff = forecast.payToday() - forecast.currentRate();
        String title = "At today's prestige, review #" + forecast.reviewNumber() + " pays " + money(forecast.payToday())
            + " a season.";
        String text = diff >= 0 ? "That's " + money(diff) + " more than now."
            : "That's " + money(-diff) + " less than now." + (forecast.keepPrestige() >= 0
                ? String.format(Locale.ROOT, " Reach %,d prestige by %s to keep %s.", forecast.keepPrestige(), date,
                money(forecast.currentRate())) : "");
        card.add(ui.notice(diff >= 0 ? Theme.NoticeKind.SUCCESS : Theme.NoticeKind.WARNING, title, text))
            .padBottom(Theme.SPACE_3).row();
        if (forecast.rivalCredit() > 0) {
            card.add(ui.paragraph(String.format(Locale.ROOT, "Rivals beat you to a First, and the government wants an "
                + "answer: this review counts %,d more prestige. The bracket above already shows it.",
                forecast.rivalCredit()), Theme.TextStyle.CAPTION, Theme.SUCCESS)).padBottom(Theme.SPACE_2).row();
        }
        card.add(ui.paragraph(lastReviewText() + " Converting prestige to funds lowers what the next review sees.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        column.add(card).padBottom(Theme.SPACE_4).row();
        column.add(scrollPane(reviewTable(forecast.reviewNumber()))).expand().fill();
        return column;
    }

    private String lastReviewText() {
        PlayerSnapshot state = session.state();
        int last = lastReviewNumber();
        if (last == 0) {
            return "No review yet: funding is the starting " + money(state.getFundingRatePerSeason()) + " a season.";
        }
        return String.format(Locale.ROOT, "Last review: #%d, at %,d prestige: %s a season.", last,
            state.getPrestigeAtLastReview(), money(state.getFundingRatePerSeason()));
    }

    private Table legendItem(Color color, String text) {
        Table item = new Table();
        item.add(new Image(ui.solid(color))).size(10f).padRight(5f);
        item.add(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK));
        return item;
    }

    private Table reviewTable(int next) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table title = new Table();
        title.add(ui.label("FUNDING REVIEWS", Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        title.add(ui.label("the same table for every agency", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(title).padBottom(Theme.SPACE_2).row();
        Table grid = new Table();
        grid.defaults().fill().padBottom(Theme.BORDER_THIN);
        grid.setBackground(ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.18f)));
        for (String head : new String[] {"Review", "Prestige", "Funding per season"}) {
            Table cell = new Table();
            cell.setBackground(ui.solid(Theme.NAVY));
            cell.pad(8f, 12f, 8f, 12f);
            cell.add(ui.label(head.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.ON_FILL)).expandX().left();
            grid.add(cell).expandX().uniformX();
        }
        grid.row();
        List<BudgetSchedule.Review> reviews = schedule().reviews();
        for (int i = 0; i < reviews.size(); i++) {
            BudgetSchedule.Review review = reviews.get(i);
            boolean upcoming = i + 1 == Math.min(next, reviews.size());
            Color fill = upcoming ? new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f) : Theme.SURFACE_CARD;
            Theme.TextStyle style = upcoming ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY_SM;
            String[] cells = {"#" + (i + 1) + (i + 1 == reviews.size() ? " and later" : ""),
                String.format(Locale.ROOT, "%,d – %,d", review.minPrestige(), review.maxPrestige()),
                money(review.minFunding()) + " – " + money(review.maxFunding())};
            for (String text : cells) {
                Table cell = new Table();
                cell.setBackground(ui.solid(fill));
                cell.pad(6f, 12f, 6f, 12f);
                cell.add(ui.label(text, style, Theme.INK)).expandX().left();
                grid.add(cell);
            }
            grid.row();
        }
        card.add(grid).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("If your prestige at a review was below its bottom, the next review's range shifts down by "
            + "the shortfall, so falling behind once doesn't compound. After the last review in the table, its range "
            + "repeats.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        return card;
    }

    // ---- Column 3: active effects and news

    private Table newsColumn() {
        PlayerSnapshot state = session.state();
        Table column = new Table();
        column.top();
        column.defaults().growX();
        boolean cost = state.getCostMultiplierSeasonsRemaining() > 0;
        boolean rd = state.getRdResultsSeasonsRemaining() > 0;
        if (cost || rd) {
            Table effects = ui.card();
            effects.defaults().growX().left();
            effects.add(ui.label("ACTIVE EFFECTS", Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(Theme.SPACE_2).row();
            if (cost) {
                double multiplier = state.getCostMultiplier();
                long saving = BudgetProgress.costDetail(state, session.catalog()).saving();
                effects.add(ui.notice(multiplier < 1 ? Theme.NoticeKind.SUCCESS : Theme.NoticeKind.WARNING,
                    (multiplier < 1 ? "Reduced expenditures" : "Cost increase") + " · "
                        + seasons(state.getCostMultiplierSeasonsRemaining()),
                    String.format(Locale.ROOT, "Running costs × %.2f: %s %s each season.", multiplier,
                        money(Math.abs(saving)), saving >= 0 ? "less" : "more"))).padBottom(Theme.SPACE_2).row();
            }
            if (rd) {
                double multiplier = state.getRdResultsMultiplier();
                effects.add(ui.notice(multiplier >= 1 ? Theme.NoticeKind.SUCCESS : Theme.NoticeKind.WARNING,
                    (multiplier >= 1 ? "R&D boost" : "R&D setback") + " · "
                        + seasons(state.getRdResultsSeasonsRemaining()),
                    String.format(Locale.ROOT, "Research gains × %.2f, for rockets and payloads alike.", multiplier)))
                    .row();
            }
            column.add(effects).padBottom(Theme.SPACE_4).row();
        }
        Table news = ui.card();
        news.top();
        news.defaults().growX().left();
        Table title = new Table();
        title.add(ui.label("NEWS", Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        title.add(ui.label("random events, newest first", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        news.add(title).padBottom(Theme.SPACE_2).row();
        List<NewsEntry> log = new ArrayList<>(state.getNewsLog());
        java.util.Collections.reverse(log);
        if (log.isEmpty()) {
            news.add(ui.paragraph("Nothing yet. Random events, good and bad, are listed here as they happen.",
                Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (int i = 0; i < log.size(); i++) {
            NewsEntry entry = log.get(i);
            TurnReportView.Line line = TurnReportView.newsLine(entry.kind(), entry.subjectId(), entry.magnitude(),
                entry.storyId(), entry.subjectName(), session.catalog(), state.getArchetype(), false);
            Table item = new Table();
            item.top().left();
            item.add(ui.label(GameDate.ofTurn(entry.turn()).label(), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .width(90f).top().left().padTop(2f);
            Image arrow = new Image(ui.icon("chevron"));
            arrow.setColor(line.tone() > 0 ? Theme.SUCCESS : Theme.DANGER);
            arrow.setOrigin(6f, 6f);
            arrow.setRotation(line.tone() > 0 ? 90f : -90f);
            item.add(arrow).size(12f).top().padTop(4f).padRight(Theme.SPACE_2);
            Table text = new Table();
            text.left();
            text.defaults().left().growX();
            text.add(ui.paragraph(line.title(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            // The narrow column shows what happened; a story's sentences sit in the tooltip (2026-10-10).
            text.add(ui.paragraph(line.tag().isEmpty() ? line.text() : line.tag(), Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED));
            if (!line.tag().isEmpty()) {
                ui.tooltip(text, line.title(), line.text());
            }
            item.add(text).expandX().fillX();
            news.add(item).padTop(8f).padBottom(8f).row();
            if (i < log.size() - 1) {
                news.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
            }
        }
        column.add(scrollPane(news)).expand().fill();
        return column;
    }

    private static String seasons(int count) {
        return count + (count == 1 ? " more season" : " more seasons");
    }

    // ---- Convert prestige

    /**
     * Opens the Convert prestige dialog, as its button does (only while in debt).
     */
    public void openConvert() {
        PlayerSnapshot state = session.state();
        long cap = PrestigeConversion.maxConversion(state, session.catalog());
        long affordable = schedule().affordableFunds(state.getPrestige());
        long max = Math.min(cap, affordable);
        if (state.getFunds() >= 0 || max <= 0) {
            toast(state.getFunds() >= 0 ? "Prestige can only be converted while in debt."
                : "Not enough prestige to convert.", Theme.NoticeKind.INFO);
            return;
        }
        showDialog(convertCard(max, max, cap, affordable), 580f);
    }

    private Table convertCard(long amount, long max, long cap, long affordable) {
        PlayerSnapshot state = session.state();
        long perPoint = schedule().fundsPerPrestige();
        Table card = dialogCard("Public Affairs Office", "Convert prestige to funds");
        card.add(ui.paragraph("Only while in debt. Each prestige point buys " + money(perPoint) + ", and the prestige "
            + "is gone for good, including at the next funding review.", Theme.TextStyle.BODY, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_4).row();
        Table head = new Table();
        head.add(ui.label("FUNDS TO RAISE", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX().left();
        Label value = ui.label(money(amount), Theme.TextStyle.BODY_STRONG, Theme.INK);
        head.add(value).right();
        card.add(head).padBottom(Theme.SPACE_2).row();
        Slider slider = new Slider(1f, max, 1f, false, ui.slider());
        slider.setValue(amount);
        card.add(slider).padBottom(2f).row();
        Table ends = new Table();
        ends.add(ui.label(money(1), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        ends.add(ui.label(money(max) + (max == affordable ? ", all your prestige" : ", the most allowed"),
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        card.add(ends).padBottom(Theme.SPACE_4).row();

        Table lines = new Table();
        lines.defaults().growX();
        card.add(lines).padBottom(Theme.SPACE_4).row();
        Runnable fill = () -> {
            long chosen = Math.round(slider.getValue());
            value.setText(money(chosen));
            lines.clearChildren();
            long price = schedule().prestigeCost(chosen);
            lines.add(row("Funds", money(state.getFunds()) + " → " + money(state.getFunds() + chosen), Theme.INK,
                Theme.TextStyle.BODY_SM)).row();
            lines.add(row("Prestige, at " + money(perPoint) + " a point", String.format(Locale.ROOT, "%,d → %,d",
                state.getPrestige(), state.getPrestige() - price), Theme.INK, Theme.TextStyle.BODY_SM)).row();
            BudgetProgress.ReviewForecast after = BudgetProgress.forecast(state, schedule(), state.getPrestige() - price);
            lines.add(row("Review #" + after.reviewNumber() + " would then pay", money(BudgetProgress.forecast(state,
                schedule()).payToday()) + " → " + money(after.payToday()), Theme.INK, Theme.TextStyle.BODY_SM)).row();
            lines.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_EMPHASIS).padTop(4f).row();
            lines.add(row("Price", String.format(Locale.ROOT, "%,d prestige", price), Theme.INK,
                Theme.TextStyle.HEADING_SM)).padTop(4f).row();
        };
        fill.run();
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                fill.run();
            }
        });
        long running = BudgetProgress.runningCosts(state, session.catalog());
        card.add(ui.notice(Theme.NoticeKind.INFO, "How much you can convert.", String.format(Locale.ROOT,
            "The %d-season limit allows up to %s: enough to bring funds to %d seasons of running costs (%s a season). "
                + "Your %,d prestige covers %s.", PrestigeConversion.COVERED_SEASONS, money(cap),
            PrestigeConversion.COVERED_SEASONS, money(running), state.getPrestige(), money(affordable))))
            .padBottom(Theme.SPACE_3).row();
        long shortfall = running - state.getFundingRatePerSeason();
        if (shortfall > 0) {
            card.add(ui.notice(Theme.NoticeKind.WARNING, "Still short next season.", "Running costs are "
                + money(shortfall) + " a season more than funding. Converting buys time; closing a program, "
                + "dismissing or a salary cut fixes it.")).padBottom(Theme.SPACE_3).row();
        }
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        TextButton convert = actionButton("Convert", Theme.ButtonKind.PRIMARY, () -> {
            long chosen = Math.round(slider.getValue());
            submit(new ConvertPrestigeAction(chosen), result -> {
                closeDialog();
                toast("Converted " + String.format(Locale.ROOT, "%,d", schedule().prestigeCost(chosen)) + " prestige into "
                    + money(chosen) + ".", Theme.NoticeKind.SUCCESS);
            });
        });
        buttons.add(convert);
        card.add(buttons).right().fill(false, false);
        return card;
    }

    /**
     * The next review's prestige range as a bar: filled up to today's prestige, with a navy mark
     * for today and a gold mark for what keeps today's funding.
     */
    private final class ReviewScale extends Widget {
        private final BudgetProgress.ReviewForecast forecast;
        private final long prestige;
        private final Drawable track = ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.15f));
        private final Drawable fill = ui.solid(Theme.NAVY);
        private final Drawable keep = ui.solid(Theme.GOLD);

        ReviewScale(BudgetProgress.ReviewForecast forecast, long prestige) {
            this.forecast = forecast;
            this.prestige = prestige;
        }

        private float at(long value) {
            long min = Math.max(0, forecast.minPrestige());
            long max = Math.max(min + 1, forecast.maxPrestige());
            float share = Math.max(0f, Math.min(1f, (float) (value - min) / (max - min)));
            return getX() + share * getWidth();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color before = batch.getColor().cpy();
            batch.setColor(1f, 1f, 1f, parentAlpha);
            float barY = getY() + getHeight() / 2f - 4f;
            track.draw(batch, getX(), barY, getWidth(), 8f);
            fill.draw(batch, getX(), barY, at(prestige) - getX(), 8f);
            if (forecast.keepPrestige() >= 0) {
                keep.draw(batch, at(forecast.keepPrestige()) - 1f, getY(), 2f, getHeight());
            }
            fill.draw(batch, at(prestige) - 1f, getY(), 2f, getHeight());
            batch.setColor(before);
        }

        @Override
        public float getPrefHeight() {
            return 30f;
        }
    }
}
