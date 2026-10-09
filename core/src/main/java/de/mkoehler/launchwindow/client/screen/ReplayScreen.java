package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.rules.ExpertRescueApplied;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.MissionPrestigeChanged;
import de.mkoehler.launchwindow.rules.MissionReliabilityChanged;
import de.mkoehler.launchwindow.rules.MissionSkillGrowthApplied;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The mission replay ({@code ui-spec.md} &sect;4.7.3): plays a resolved season's steps one by one,
 * each with its components as flown, the chances, which rolls passed, any Expert rescue and the
 * outcome, then the season summary. Nothing is rolled here: every step is an event the season pass
 * already resolved. The player steps through at their own pace (user decision 2026-10-06: no
 * automatic playback): previous and next, a click on any step, or straight to the summary.
 *
 * @author Mario Koehler
 */
public final class ReplayScreen extends ChromeScreen {

    private final TurnReport report;
    private String missionId;
    /** The step shown, from 0; the step count means the season summary. */
    private int shown;

    /**
     * Builds the replay of one mission's season.
     *
     * @param game      the game, in a session
     * @param report    the turn report it comes from
     * @param missionId the mission
     */
    public ReplayScreen(LaunchWindowGame game, TurnReport report, String missionId) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Turn report", screen -> screen.game.setScreen(
            new TurnReportScreen(screen.game, report))), new Crumb("Replay", null)), "Mission Control",
            "Mission replay");
        this.report = report;
        this.missionId = missionId;
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(new TurnReportScreen(game, report));
    }

    private List<MissionStepResolved> steps() {
        return TurnReportView.steps(report, missionId);
    }

    /**
     * Shows a step, or the season summary for the step count.
     *
     * @param step the step, from 0
     */
    public void show(int step) {
        shown = Math.max(0, Math.min(step, steps().size()));
        rebuild();
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.TURN_REPORT;
    }

    @Override
    protected void rebuild() {
        List<MissionStepResolved> steps = steps();
        titleActions.clearChildren();
        titleActions.add(transport("restart", () -> show(0))).size(36f).padRight(6f);
        titleActions.add(transport("step_back", () -> show(shown - 1))).size(36f).padRight(6f);
        if (shown < steps.size()) {
            titleActions.add(actionButton(shown == steps.size() - 1 ? "Season summary" : "Next step",
                Theme.ButtonKind.PRIMARY, () -> show(shown + 1))).height(36f).padRight(Theme.SPACE_3);
            titleActions.add(onClick(ui.pageButton("Skip to summary"), () -> show(steps.size())));
        }

        content.clearChildren();
        content.top();
        content.defaults().growX();
        Table chooser = new Table();
        for (TurnReport.MissionFlown flown : report.missions()) {
            boolean active = flown.missionInstanceId().equals(missionId);
            Table chip = new Table();
            chip.setBackground(active ? ui.solid(Theme.SURFACE_CARD)
                : ui.rect(new Color(0f, 0f, 0f, 0f), Theme.ON_PAGE, Theme.BORDER_THIN));
            chip.pad(5f, 12f, 5f, 12f);
            String name = session.catalog().missionConfiguration(flown.missionConfigurationId())
                .map(MissionControl::missionName).orElse(ContentNames.of(flown.missionConfigurationId()));
            chip.add(ui.label(name, Theme.TextStyle.LABEL, active ? Theme.NAVY : Theme.ON_PAGE_STRONG));
            chip.setTouchable(Touchable.enabled);
            chip.addListener(clicked(() -> {
                missionId = flown.missionInstanceId();
                shown = 0;
                rebuild();
            }));
            chooser.add(chip).padRight(6f);
        }
        chooser.add(ui.label("Replays from the last turn (" + GameDate.ofTurn(report.gameTurn()).label() + " → "
            + GameDate.ofTurn(report.gameTurn() + 1).label() + "). Also opened from the turn report.",
            Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG)).expandX().left().padLeft(Theme.SPACE_3);
        content.add(chooser).padBottom(Theme.SPACE_4).row();

        Table columns = new Table();
        columns.add(scrollPane(stepList(steps))).width(380f).top().fillY().padRight(Theme.SPACE_5);
        if (shown < steps.size()) {
            columns.add(scrollPane(stepDetail(steps, shown))).expand().fill().padRight(Theme.SPACE_5);
            columns.add(side(steps)).width(320f).top();
        } else {
            columns.add(scrollPane(summary(steps))).expand().fill();
        }
        content.add(columns).expand().fill();
    }

    private Table transport(String icon, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        button.add(glyph).size(16f);
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(action));
        return button;
    }

    private TurnReport.MissionFlown flown() {
        return report.missions().stream().filter(mission -> mission.missionInstanceId().equals(missionId)).findFirst()
            .orElse(null);
    }

    private Table stepList(List<MissionStepResolved> steps) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        TurnReport.MissionFlown flown = flown();
        Table head = new Table();
        head.add(ui.label(flown == null ? "" : ("Season " + (flown.seasonIndex() + 1) + " of " + flown.seasonsTotal())
            .toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).expandX().left();
        head.add(ui.label(steps.size() + " steps", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(head).padBottom(Theme.SPACE_3).row();
        for (int i = 0; i < steps.size(); i++) {
            MissionStepResolved step = steps.get(i);
            boolean revealed = i <= shown;
            Table row = new Table();
            if (i == shown) {
                row.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.06f), Theme.NAVY,
                    Theme.BORDER_THIN));
            }
            row.pad(6f, 8f, 6f, 8f);
            row.add(ui.label(String.valueOf(i + 1), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(22f).left();
            row.add(ui.label(TurnReportView.stepName(step.missionConfigurationId(), step.stepId()), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            row.add(ui.label(revealed ? outcomeShort(step.outcome()) : "—", Theme.TextStyle.LABEL,
                revealed ? outcomeColor(step.outcome()) : Theme.INK_MUTED));
            int index = i;
            row.setTouchable(Touchable.enabled);
            row.addListener(clicked(() -> show(index)));
            card.add(row).padBottom(2f).row();
        }
        Table summaryRow = new Table();
        if (shown >= steps.size()) {
            summaryRow.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.06f), Theme.NAVY,
                Theme.BORDER_THIN));
        }
        summaryRow.pad(6f, 8f, 6f, 8f);
        summaryRow.add(ui.label("Σ", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(22f).left();
        summaryRow.add(ui.label("Season summary", Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
        summaryRow.setTouchable(Touchable.enabled);
        summaryRow.addListener(clicked(() -> show(steps.size())));
        card.add(summaryRow).padTop(Theme.SPACE_2).row();
        return card;
    }

    private static String outcomeShort(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL -> "Success";
            case MINOR_GLITCH_SOLVED -> "Saved";
            case MINOR_GLITCH -> "Glitch";
            case FAILURE -> "Failure";
            case CATASTROPHIC -> "Catastrophic";
        };
    }

    private static Color outcomeColor(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL, MINOR_GLITCH_SOLVED -> Theme.SUCCESS;
            case MINOR_GLITCH -> Theme.GOLD_TEXT;
            case FAILURE, CATASTROPHIC -> Theme.DANGER;
        };
    }

    private Table stepDetail(List<MissionStepResolved> steps, int index) {
        MissionStepResolved step = steps.get(index);
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(ui.label(("Step " + (index + 1) + " of " + steps.size()).toUpperCase(Locale.ROOT),
            Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        card.add(ui.label(TurnReportView.stepName(step.missionConfigurationId(), step.stepId()),
            Theme.TextStyle.HEADING_LG, Theme.INK)).padBottom(Theme.SPACE_4).row();
        String about = ContentNames.stepDescription(step.missionConfigurationId(), step.stepId());
        if (about != null) {
            Label text = ui.paragraph(about, Theme.TextStyle.BODY_SM, Theme.INK_MUTED);
            card.add(text).padTop(-Theme.SPACE_2).padBottom(Theme.SPACE_4).row();
        }
        for (MissionStepResolved.ComponentFlown component : step.components()) {
            Optional<MissionComponent> spec = session.catalog().missionComponent(component.componentId());
            Table head = new Table();
            if (spec.isPresent()) {
                Image glyph = new Image(ui.icon(SetSkills.icon(spec.get().associatedSkill())), Scaling.fit);
                glyph.setColor(Theme.NAVY);
                head.add(glyph).size(14f).padRight(6f);
            }
            head.add(ui.label(ContentNames.of(component.componentId()), Theme.TextStyle.BODY_STRONG, Theme.INK))
                .expandX().left();
            head.add(ui.label(component.percent() + "% of this step", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            card.add(head).padBottom(4f).row();
            if (spec.isPresent()) {
                card.add(new ReliabilityBar(ui, component.reliability(), spec.get().maxRdReliability(),
                    spec.get().maxReliability(), 0)).height(12f).padBottom(2f).row();
            }
            card.add(ui.label(String.format(Locale.ROOT, "%.1f%% as flown", component.reliability()),
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        }
        Table chances = new Table();
        chances.left();
        chances.add(big(String.format(Locale.ROOT, "%.0f%%", step.successChance()), "Success chance"))
            .padRight(Theme.SPACE_6);
        chances.add(big(step.savingChance() == null ? "—" : String.format(Locale.ROOT, "%.0f%%",
            step.savingChance()), "Controllers' saving roll"));
        card.add(chances).padBottom(Theme.SPACE_4).row();

        card.add(roll(step.firstRollPassed(), "First roll", String.format(Locale.ROOT, "%.0f%% to pass",
            step.successChance()))).padBottom(4f).row();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof ExpertRescueApplied rescue && rescue.missionInstanceId().equals(missionId)
                && rescue.stepId().equals(step.stepId())) {
                Table row = new Table();
                Image star = new Image(ui.icon("prestige"), Scaling.fit);
                star.setColor(Theme.GOLD_TEXT);
                row.add(star).size(14f).padRight(Theme.SPACE_2);
                row.add(ui.paragraph(String.format(Locale.ROOT, "%s, an Expert in %s, raised the saving roll from "
                    + "%.0f%% to %.0f%%.", rescue.name(), skill(rescue.skillName()), rescue.probabilityBefore(),
                    rescue.probabilityAfter()), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
                card.add(row).padBottom(4f).row();
            }
        }
        if (step.savingRollPassed() != null) {
            card.add(roll(step.savingRollPassed(), "Saving roll", String.format(Locale.ROOT, "%.0f%% to pass",
                step.savingChance()))).padBottom(4f).row();
        }
        card.add(ui.notice(noticeKind(step.outcome()), outcomeTitle(step.outcome()), outcomeText(step.outcome())))
            .padTop(Theme.SPACE_3);
        return card;
    }

    private String skill(String skill) {
        return de.mkoehler.launchwindow.client.view.CrewSkills.label(skill);
    }

    private Table big(String value, String label) {
        Table stat = new Table();
        stat.defaults().left();
        stat.add(ui.label(value, Theme.TextStyle.HEADING_LG, Theme.INK)).row();
        stat.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED));
        return stat;
    }

    private Table roll(boolean passed, String text, String value) {
        Table row = new Table();
        Image mark = new Image(ui.icon(passed ? "check" : "lock"), Scaling.fit);
        mark.setColor(passed ? Theme.SUCCESS : Theme.DANGER);
        row.add(mark).size(14f).padRight(Theme.SPACE_2);
        row.add(ui.label(text + (passed ? " passed" : " failed"), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
        row.add(ui.label(value, Theme.TextStyle.BODY_STRONG, Theme.INK));
        return row;
    }

    private static Theme.NoticeKind noticeKind(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL, MINOR_GLITCH_SOLVED -> Theme.NoticeKind.SUCCESS;
            case MINOR_GLITCH -> Theme.NoticeKind.WARNING;
            case FAILURE, CATASTROPHIC -> Theme.NoticeKind.DANGER;
        };
    }

    private static String outcomeTitle(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL -> "Success.";
            case MINOR_GLITCH_SOLVED -> "Glitch, saved by the controllers.";
            case MINOR_GLITCH -> "Minor glitch.";
            case FAILURE -> "Failure.";
            case CATASTROPHIC -> "Catastrophic failure.";
        };
    }

    private static String outcomeText(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL -> "The step went as planned.";
            case MINOR_GLITCH_SOLVED -> "Something went wrong, and mission control caught it. Full prestige.";
            case MINOR_GLITCH -> "The mission goes on, at half the step's prestige.";
            case FAILURE -> "The mission ends here.";
            case CATASTROPHIC -> "The mission ends here, and the crew is lost.";
        };
    }

    private Table side(List<MissionStepResolved> steps) {
        Table side = new Table();
        side.top();
        side.defaults().growX().padBottom(Theme.SPACE_4);
        Table prestige = ui.card();
        prestige.defaults().left();
        prestige.add(ui.label("PRESTIGE SO FAR", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        prestige.add(ui.label(TurnReportView.signed(steps.get(shown).seasonPrestigeAfter()), Theme.TextStyle.HEADING_LG,
            Theme.INK)).padBottom(2f).row();
        prestige.add(ui.paragraph("This season's running total. A failing step replaces it with its own deduction.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        side.add(prestige).row();
        Table tally = ui.card();
        tally.defaults().growX().left();
        tally.add(ui.label("SO FAR THIS SEASON", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Map<MissionStepOutcome, Integer> counts = new EnumMap<>(MissionStepOutcome.class);
        for (int i = 0; i <= shown && i < steps.size(); i++) {
            counts.merge(steps.get(i).outcome(), 1, Integer::sum);
        }
        for (MissionStepOutcome outcome : MissionStepOutcome.values()) {
            if (counts.containsKey(outcome)) {
                Table row = new Table();
                row.add(ui.label(outcomeShort(outcome), Theme.TextStyle.BODY_SM, outcomeColor(outcome))).expandX().left();
                row.add(ui.label(String.valueOf(counts.get(outcome)), Theme.TextStyle.BODY_SM, Theme.INK));
                tally.add(row).padBottom(2f).row();
            }
        }
        side.add(tally).row();
        return side;
    }

    private Table summary(List<MissionStepResolved> steps) {
        TurnReport.MissionFlown flown = flown();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        MissionStepOutcome last = steps.isEmpty() ? MissionStepOutcome.SUCCESSFUL : steps.get(steps.size() - 1).outcome();
        boolean failed = last == MissionStepOutcome.FAILURE || last == MissionStepOutcome.CATASTROPHIC;
        String headline = failed ? "Ended on step " + steps.size()
            : flown != null && flown.ended() ? "Mission complete"
            : flown == null ? "Season done" : "Season " + (flown.seasonIndex() + 1) + " of " + flown.seasonsTotal()
                + " done";
        long prestige = report.events().stream().filter(MissionPrestigeChanged.class::isInstance)
            .map(MissionPrestigeChanged.class::cast).filter(event -> event.missionInstanceId().equals(missionId))
            .mapToLong(MissionPrestigeChanged::applied).sum();
        Table head = new Table();
        Table titles = new Table();
        titles.defaults().left();
        titles.add(ui.label("SEASON SUMMARY", Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        titles.add(ui.label(headline, Theme.TextStyle.HEADING_LG, Theme.INK));
        head.add(titles).expandX().left();
        head.add(big(TurnReportView.signed(prestige), "Prestige this season"));
        card.add(head).padBottom(Theme.SPACE_4).row();

        List<String> goals = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof GoalSettled goal && goal.missionInstanceId().equals(missionId)) {
                goals.add(ContentNames.of(goal.goalId()) + (goal.firstInGame() ? " (first in the game)" : "")
                    + (goal.cancelled() ? ", credited" : ""));
            }
        }
        section(card, "Goals", goals.isEmpty() ? "None this season." : String.join("; ", goals) + ".");
        List<String> lost = report.events().stream().filter(PersonnelDeparture.class::isInstance)
            .map(PersonnelDeparture.class::cast)
            .filter(departure -> flown != null && flown.crewNames().contains(departure.name()))
            .map(PersonnelDeparture::name).toList();
        String crew = flown == null || flown.crewNames().isEmpty() ? "Uncrewed."
            : !lost.isEmpty() ? String.join(" and ", lost) + (lost.size() == 1 ? " was" : " were") + " lost."
            : String.join(" and ", flown.crewNames()) + (flown.ended() ? " came home safely." : " are still aboard.");
        section(card, "Crew", crew);
        card.add(ui.label("RELIABILITY CHANGES", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(Theme.SPACE_2)
            .padBottom(Theme.SPACE_2).row();
        boolean any = false;
        for (ResolutionEvent event : report.events()) {
            if (event instanceof MissionReliabilityChanged change && change.missionInstanceId().equals(missionId)) {
                any = true;
                Table row = new Table();
                row.add(ui.label(ContentNames.of(change.componentId()), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX()
                    .left();
                row.add(ui.label(String.format(Locale.ROOT, "%s %.1f", change.delta() < 0 ? "−" : "+",
                    Math.abs(change.delta())), Theme.TextStyle.BODY_STRONG, change.delta() < 0 ? Theme.DANGER
                    : Theme.SUCCESS));
                card.add(row).padBottom(2f).row();
                report.components().stream().filter(c -> c.componentId().equals(change.componentId())).findFirst()
                    .ifPresent(c -> session.catalog().missionComponent(c.componentId()).ifPresent(spec -> card.add(
                        new ReliabilityBar(ui, c.after(), spec.maxRdReliability(), spec.maxReliability(), 0))
                        .height(12f).padBottom(Theme.SPACE_2).row()));
            }
        }
        if (!any) {
            card.add(ui.paragraph("None.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        }
        card.add(ui.label("SKILL GROWTH", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(Theme.SPACE_2)
            .padBottom(Theme.SPACE_2).row();
        boolean grew = false;
        for (ResolutionEvent event : report.events()) {
            if (event instanceof MissionSkillGrowthApplied growth && growth.missionInstanceId().equals(missionId)) {
                grew = true;
                Table row = new Table();
                row.add(ui.label(growth.name(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
                row.add(ui.label(String.format(Locale.ROOT, "+ %.1f", growth.totalSkillGain()), Theme.TextStyle.BODY_SM,
                    Theme.SUCCESS));
                card.add(row).padBottom(2f).row();
            }
        }
        if (!grew) {
            card.add(ui.paragraph("None.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        return card;
    }

    private void section(Table card, String title, String text) {
        card.add(ui.label(title.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        card.add(ui.paragraph(text, Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(Theme.SPACE_3).row();
    }
}
