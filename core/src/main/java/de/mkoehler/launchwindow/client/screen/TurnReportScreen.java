package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.client.view.ContractsView;
import de.mkoehler.launchwindow.client.view.DecisionView;
import de.mkoehler.launchwindow.rules.ContractSettled;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.content.SpaceRaceNews;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.MilestoneLabels;
import de.mkoehler.launchwindow.client.ui.OutcomeBar;
import de.mkoehler.launchwindow.client.ui.RelationshipMarker;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.TraitChip;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.ResearchPlanning;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The turn report ({@code ui-spec.md} &sect;4.4): "what happened overnight". An at-a-glance strip,
 * then three columns: missions flown (each with its steps' outcomes and a replay), goals and
 * buildings; R&amp;D and news; people and the budget. When R&amp;D shows more than one component, the
 * news and intelligence move to the left column, under the missions. Built from one {@link TurnReport}.
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
    protected Help.Context helpContext() {
        return Help.Context.TURN_REPORT;
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
        Table decision = decisionCard();
        if (decision != null) {
            content.add(decision).padBottom(Theme.SPACE_4).row();
        }
        for (ContractsView.ReportCard contract : ContractsView.reportCards(report)) {
            content.add(contractCard(contract)).padBottom(Theme.SPACE_4).row();
        }

        Table columns = new Table();
        columns.top();
        // With more than one component's drawing, R&D fills the middle column, so the news and the
        // intelligence findings move up the left one, under the missions (the user's request, 2026-10-10).
        boolean newsLeft = TurnReportView.rnd(report).size() > NEWS_MOVES_AFTER_COMPONENTS;
        List<TurnReportView.Line> intelligence = TurnReportView.intelligence(report);
        Table first = column();
        first.add(missions(catalog)).row();
        Table second = column();
        second.add(rnd(catalog)).row();
        Table newsColumn = newsLeft ? first : second;
        newsColumn.add(news(catalog)).row();
        if (!intelligence.isEmpty()) {
            newsColumn.add(intelligence(intelligence)).row();
        }
        first.add(goals()).row();
        first.add(buildings()).row();
        Table third = column();
        third.add(people(catalog)).row();
        third.add(budget()).row();
        columns.add(first).expandX().fillX().uniformX().top().padRight(Theme.SPACE_4);
        columns.add(second).expandX().fillX().uniformX().top().padRight(Theme.SPACE_4);
        columns.add(third).expandX().fillX().uniformX().top();
        content.add(scrollPane(columns)).expand().fill();
    }

    /** More components than this in the R&amp;D section move the news to the left column. */
    private static final int NEWS_MOVES_AFTER_COMPONENTS = 1;

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
        // A seventh cell while a decision waits (ui-spec.md component 26): gold-tinted, a gold top line.
        PendingDecision pending = waitingDecision();
        if (pending != null) {
            strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
            Table cell = new Table();
            cell.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.10f), Theme.GOLD,
                Theme.BORDER_THIN));
            cell.left();
            cell.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
            cell.defaults().left();
            cell.add(ui.label("DECISION", Theme.TextStyle.CAPTION, Theme.GOLD_TEXT)).padBottom(2f).row();
            cell.add(ui.label("1 waiting", Theme.TextStyle.HEADING_SM, Theme.INK)).row();
            cell.add(ui.label(ContentNames.of(pending.componentId()), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            strip.add(cell).expandX().fillX().uniformX().fillY().top();
        }
        // A cell for the agency's own contract news (ui-spec.md section 4.17), its top line in the card's tone.
        List<ContractsView.ReportCard> contracts = ContractsView.reportCards(report);
        if (!contracts.isEmpty()) {
            ContractsView.ReportCard first = contracts.getFirst();
            strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
            Table cell = new Table();
            cell.setBackground(ui.solid(Theme.SURFACE_CARD));
            cell.top().left();
            cell.add(new Image(ui.solid(contractTone(first.kind())))).height(Theme.BORDER_ACCENT).growX().colspan(1)
                .row();
            Table words = new Table();
            words.left();
            words.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
            words.defaults().left();
            words.add(ui.label("CONTRACTS", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
            words.add(ui.label(contractCell(first), Theme.TextStyle.HEADING_SM, Theme.INK)).row();
            words.add(ui.label(first.title(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            cell.add(words).growX();
            strip.add(cell).expandX().fillX().uniformX().fillY().top();
        }
        return strip;
    }

    /** A contract outcome's accent: gold awarded, green paid, navy refunded, red lapsed, muted otherwise. */
    private static Color contractTone(ContractSettled.Kind kind) {
        return switch (kind) {
            case AWARDED -> Theme.GOLD;
            case PAID -> Theme.SUCCESS;
            case REFUNDED -> Theme.NAVY;
            case LAPSED -> Theme.DANGER;
            case MISSED, VOIDED -> Theme.NAVY_BORDER;
        };
    }

    private static String contractCell(ContractsView.ReportCard card) {
        return switch (card.kind()) {
            case AWARDED -> "Awarded";
            case MISSED -> "Missed out";
            case VOIDED -> "Came to nothing";
            case PAID -> "Paid";
            case REFUNDED -> "Insured";
            case LAPSED -> "Lapsed";
        };
    }

    /**
     * A Contracts card above the columns ({@code ui-spec.md} &sect;4.17): what happened to the agency's
     * application or contract, with a left accent in its tone.
     */
    private Table contractCard(ContractsView.ReportCard contract) {
        Table card = new Table();
        card.setBackground(ui.cardAccent(contractTone(contract.kind())));
        card.pad(Theme.SPACE_4, Theme.SPACE_4 + Theme.BORDER_ACCENT + 4f, Theme.SPACE_4, Theme.SPACE_5);
        Table words = new Table();
        words.left();
        words.defaults().left().growX();
        words.add(ui.label(("Contract · " + contractCell(contract)).toUpperCase(Locale.ROOT),
            Theme.TextStyle.EYEBROW, contract.kind() == ContractSettled.Kind.AWARDED ? Theme.GOLD_TEXT : Theme.NAVY))
            .padBottom(4f).row();
        words.add(ui.paragraph(contract.title(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        for (String line : contract.lines()) {
            words.add(ui.paragraph(line, Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(2f).row();
        }
        card.add(words).expandX().fillX().left().padRight(Theme.SPACE_5);
        Table right = new Table();
        if (!contract.value().isEmpty()) {
            right.add(ui.label(contract.value(), Theme.TextStyle.HEADING_SM, tone(contract.tone()))).right().row();
        }
        right.add(actionButton("See the board", Theme.ButtonKind.SECONDARY,
            () -> game.setScreen(new ContractsScreen(game)))).right().padTop(Theme.SPACE_2);
        card.add(right).right();
        return card;
    }

    /**
     * The decision waiting now, shown on the latest report only: an older report's decision is long
     * settled.
     */
    private PendingDecision waitingDecision() {
        if (report.gameTurn() < session.latestResolvedTurn() || !DecisionView.waiting(session.state())) {
            return null;
        }
        PendingDecision pending = session.state().getPendingDecision();
        return DecisionView.content(pending).isPresent() ? pending : null;
    }

    /**
     * The Decision card above the columns ({@code ui-spec.md} component 26): the decision waiting, the
     * story's first sentence, what applies if the player doesn't decide, and the way to the board.
     */
    private Table decisionCard() {
        PendingDecision pending = waitingDecision();
        if (pending == null) {
            return null;
        }
        Decisions.Decision decision = DecisionView.content(pending).orElseThrow();
        Table card = new Table();
        card.setBackground(ui.cardAccent(Theme.GOLD));
        card.pad(Theme.SPACE_4, Theme.SPACE_4 + Theme.BORDER_ACCENT, Theme.SPACE_4, Theme.SPACE_4);
        Table words = new Table();
        words.left();
        words.defaults().left().growX();
        words.add(ui.label(("Decision \u00b7 " + ContentNames.of(pending.componentId())).toUpperCase(Locale.ROOT),
            Theme.TextStyle.EYEBROW, Theme.GOLD_TEXT)).padBottom(4f).row();
        words.add(ui.paragraph(decision.headline(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        String text = decision.text();
        int stop = text.indexOf(". ");
        words.add(ui.paragraph(stop > 0 ? text.substring(0, stop + 1) : text, Theme.TextStyle.BODY, Theme.INK))
            .padBottom(4f).row();
        if (pending.handsOnInProgress()) {
            // Started: it settles as it stands, there's no default any more.
            words.add(ui.paragraph("Hands-on in progress: " + de.mkoehler.launchwindow.client.view.HandsOnView
                .progress(decision.handsOn(), pending.handsOn()) + ". Unfinished, it settles as it stands.",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        } else {
            String fallback = decision.defaultChoice().label();
            words.add(ui.paragraph("If you don't decide: " + fallback + ".", Theme.TextStyle.CAPTION,
                Theme.INK_MUTED));
        }
        card.add(words).expandX().fillX().left().padRight(Theme.SPACE_5);
        Table right = new Table();
        right.add(actionButton(pending.handsOnInProgress() ? "Continue the campaign" : "Decide on the R&D board",
            Theme.ButtonKind.PRIMARY, () -> game.setScreen(pending.handsOnInProgress() ? new HandsOnScreen(game)
            : new RndBoardScreen(game, DecisionView.rocket(pending.componentId(), session.catalog())
                ? RndBoardScreen.Board.ROCKET : RndBoardScreen.Board.PAYLOAD)))).row();
        right.add(ui.label(DecisionView.untilResolved(nextTurnText()), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padTop(4f);
        card.add(right).right();
        return card;
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
            TextureRegionDrawable blueprint = ui.picture("blueprints/" + line.componentId() + ".jpg");
            if (blueprint != null) {
                card.add(new Blueprint(blueprint)).padBottom(Theme.SPACE_2).row();
            }
            Table head = new Table();
            head.add(ui.label(ContentNames.of(line.componentId()), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX()
                .left();
            double delta = line.after() - line.before();
            head.add(ui.label(String.format(Locale.ROOT, "%s %.1f", delta < 0 ? "−" : "+", Math.abs(delta)),
                Theme.TextStyle.BODY_STRONG, delta < 0 ? Theme.DANGER : Theme.SUCCESS));
            card.add(head).padBottom(4f).row();
            if (component.isPresent()) {
                ReliabilityBar bar = new ReliabilityBar(ui, line.after(), component.get().maxRdReliability(),
                    component.get().maxReliability(), 0);
                // The seasons to each mark from here (component 25), on the latest report only: an older
                // one's bar shows a reliability the component has since moved on from.
                boolean latest = report.gameTurn() >= session.latestResolvedTurn();
                var owned = session.state().getMissionComponents().get(line.componentId());
                if (latest && owned != null && Math.abs(owned.getCurrentReliability() - line.after()) < 0.05) {
                    MilestoneLabels labels = RndCards.milestones(this, component.get(),
                        ResearchPlanning.project(session.state(), catalog, line.componentId()), bar);
                    if (labels != null) {
                        card.add(labels).padBottom(2f).row();
                    }
                }
                card.add(bar).height(12f).padBottom(4f).row();
            }
            card.add(ui.paragraph(String.format(Locale.ROOT, "%.1f → %.1f. %s", line.before(), line.after(),
                line.who()), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        }
        for (TurnReportView.Line capped : TurnReportView.capped(report)) {
            card.add(ui.notice(Theme.NoticeKind.SUCCESS, capped.title(), capped.text())).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    /**
     * A component's blueprint drawing ({@code ui-spec.md} &sect;4.4): as wide as the card, as tall as its
     * aspect then needs. Its preferred height follows the width the card gives it, so the card lays out
     * a second time once the width is known.
     */
    private static final class Blueprint extends Image {

        private final float aspect;

        Blueprint(TextureRegionDrawable drawing) {
            super(drawing, Scaling.fit);
            aspect = drawing.getRegion().getRegionHeight() / (float) drawing.getRegion().getRegionWidth();
        }

        @Override
        public float getPrefWidth() {
            return 0f;
        }

        @Override
        public float getPrefHeight() {
            return getWidth() * aspect;
        }

        @Override
        protected void sizeChanged() {
            super.sizeChanged();
            if (Math.abs(getHeight() - getPrefHeight()) > 0.5f) {
                invalidateHierarchy();
            }
        }
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
        List<TurnReportView.Line> lines = new java.util.ArrayList<>(TurnReportView.news(report, catalog,
            session.state().getArchetype()));
        // The contract board's public news (ui-spec.md section 4.17).
        lines.addAll(ContractsView.news(report));
        // Last turn's R&D decision, decided or lapsed (ui-spec.md component 26).
        for (var event : report.events()) {
            if (event instanceof de.mkoehler.launchwindow.rules.DecisionSettled settled) {
                DecisionView.reportLine(settled, session.state()).ifPresent(line ->
                    lines.add(new TurnReportView.Line(line[0], line[1], "", DecisionView.tone(settled))));
            }
        }
        if (lines.isEmpty()) {
            card.add(ui.paragraph("A quiet season: no random events.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (TurnReportView.Line line : lines) {
            Table row = new Table();
            // Neutral news (a decision that changed nothing) gets a navy chevron, not a red arrow.
            Image arrow = new Image(ui.icon(line.tone() > 0 ? "sort_asc" : line.tone() < 0 ? "sort_desc" : "chevron"),
                Scaling.fit);
            arrow.setColor(line.tone() > 0 ? Theme.SUCCESS : line.tone() < 0 ? Theme.DANGER : Theme.NAVY);
            row.add(arrow).size(14f).top().padRight(Theme.SPACE_2);
            Table text = new Table();
            text.left();
            text.defaults().left().growX();
            text.add(ui.paragraph(line.title(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            // A story's text, then what the event did (2026-10-10); without a story, the effect alone.
            text.add(ui.paragraph(line.text(), Theme.TextStyle.BODY_SM, line.tag().isEmpty() ? Theme.INK_MUTED
                : Theme.INK)).row();
            if (!line.tag().isEmpty()) {
                text.add(ui.paragraph(line.tag(), Theme.TextStyle.BODY_SM, line.tone() > 0 ? Theme.SUCCESS
                    : Theme.DANGER)).padTop(2f);
            }
            row.add(text).expandX().fillX();
            card.add(row).padBottom(Theme.SPACE_3).row();
        }
        return card;
    }

    private Table intelligence(List<TurnReportView.Line> lines) {
        Table card = section("Intelligence", null);
        for (TurnReportView.Line line : lines) {
            Table row = new Table();
            Image glyph = new Image(ui.icon(line.tone() > 0 ? "check" : line.tone() < 0 ? "close" : "binoculars"),
                Scaling.fit);
            glyph.setColor(line.tone() > 0 ? Theme.SUCCESS : line.tone() < 0 ? Theme.DANGER : Theme.INK_MUTED);
            row.add(glyph).size(14f).top().padRight(Theme.SPACE_2).padTop(2f);
            Table text = new Table();
            text.left();
            text.defaults().left().growX();
            text.add(ui.label(line.title(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            text.add(ui.paragraph(line.text(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            row.add(text).expandX().fillX();
            card.add(row).padBottom(Theme.SPACE_3).row();
        }
        Label open = ui.label("Open the Intelligence Office", Theme.TextStyle.CAPTION, Theme.NAVY);
        open.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        open.addListener(clicked(() -> game.setScreen(new IntelligenceOfficeScreen(game,
            IntelligenceOfficeScreen.Tab.FOLDERS))));
        card.add(open).left().row();
        return card;
    }

    private Table people(ContentCatalog catalog) {
        Table card = section("People", null);
        // Morale warnings are about now, so only the latest report shows them.
        boolean latest = report.gameTurn() >= session.latestResolvedTurn();
        List<TurnReportView.Line> lines = TurnReportView.people(report, latest ? session.state() : null, catalog);
        List<TurnReportView.PeopleNews> news = TurnReportView.peopleNews(report, catalog);
        if (lines.isEmpty() && news.isEmpty()) {
            card.add(ui.paragraph("No changes this season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        // Traits and relationships first, lightly tinted (People canvas, 2026-10-07).
        for (TurnReportView.PeopleNews line : news) {
            card.add(peopleNews(line)).padBottom(Theme.SPACE_2).row();
        }
        for (TurnReportView.Line line : lines) {
            Color color = line.tone() > 0 ? Theme.SUCCESS : line.tone() < 0 ? Theme.DANGER : Theme.INK_MUTED;
            Table row = line(line.title(), line.tag().toUpperCase(Locale.ROOT), color);
            // A line about someone still here opens their page (poaching's lines, ui-spec.md 4.16).
            Personnel person = line.personId() == null ? null : People.find(session.state(), line.personId());
            if (person != null) {
                row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
                row.addListener(clicked(() -> game.setScreen(new PersonScreen(game, person,
                    () -> new TurnReportScreen(game, report)))));
                ui.tooltip(row, person.getName(), "Opens the person page.");
            }
            card.add(row).padBottom(line.text().isEmpty() ? Theme.SPACE_2 : 2f).row();
            if (!line.text().isEmpty()) {
                card.add(ui.paragraph(line.text(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX()
                    .padBottom(Theme.SPACE_2).row();
            }
        }
        return card;
    }

    /**
     * A line of people news: who, what came out and where, ending with the trait's chip or the
     * relationship's marker. A click opens the person's page while they're still in the agency.
     */
    private Table peopleNews(TurnReportView.PeopleNews line) {
        Table row = new Table();
        row.setBackground(ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f)));
        row.pad(Theme.SPACE_2, Theme.SPACE_2, Theme.SPACE_2, Theme.SPACE_2);
        row.add(ui.paragraph(line.text(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX().left()
            .padRight(Theme.SPACE_2);
        if (line.trait() != null) {
            row.add(new TraitChip(ui, line.trait())).height(TraitChip.HEIGHT).right().top();
        } else {
            Table marker = new Table();
            marker.add(RelationshipMarker.glyph(ui, line.positive())).size(12f).padRight(4f);
            marker.add(ui.label(line.positive() ? "WORK WELL" : "CLASH", Theme.TextStyle.LABEL,
                RelationshipMarker.color(line.positive())));
            row.add(marker).right().top().padTop(2f);
        }
        Personnel person = People.find(session.state(), line.personId());
        if (person != null) {
            row.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            row.addListener(clicked(() -> game.setScreen(new PersonScreen(game, person,
                () -> new TurnReportScreen(game, report)))));
        }
        return row;
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
