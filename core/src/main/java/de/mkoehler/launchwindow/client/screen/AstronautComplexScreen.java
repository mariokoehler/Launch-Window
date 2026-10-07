package de.mkoehler.launchwindow.client.screen;

import de.mkoehler.launchwindow.rules.PrDuty;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.client.ui.HistoryNotes;
import de.mkoehler.launchwindow.actions.RecallFromPrDutyAction;
import de.mkoehler.launchwindow.actions.StartPrDutyAction;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.AstronautCorps;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.rules.SkillGrowth;

import java.util.List;
import java.util.Locale;

/**
 * The Astronaut Complex ({@code ui-spec.md} &sect;4.9, component 11): the department strip with the
 * building cell, filter chips, and one row per astronaut with their state (crew seat or CAPCOM
 * console), the five skills with Expert marks, a hint when a skill can train toward Expert, and
 * Train, Pull out, Mission and Dismiss. Hire and Manage department are its sub-screens.
 *
 * @author Mario Koehler
 */
public final class AstronautComplexScreen extends ChromeScreen {

    private static final String HEADCOUNT_NOTE = "The first Soviet cosmonaut group, chosen in 1960, was picked "
        + "partly by size. The Vostok cabin was so cramped that candidates had to be under about 1.70 m and 72 kg. "
        + "Fitness tests included centrifuge runs and days alone in a silent isolation chamber.";

    private final PersonnelDialogs dialogs = new PersonnelDialogs(this, Department.ASTRONAUT);
    private MissionControl.Filter filter = MissionControl.Filter.ALL;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public AstronautComplexScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.ASTRONAUT_COMPLEX), "Astronaut Complex", "Astronauts");
        rebuild();
    }

    @Override
    protected void goAstronautComplex() {
        // Already here.
    }

    @Override
    protected void rebuild() {
        titleActions.clearChildren();
        titleActions.add(onClick(ui.pageButton("Hire"), () -> game.setScreen(new HireScreen(game,
            Department.ASTRONAUT)))).padRight(10f);
        titleActions.add(onClick(ui.pageButton("Manage department"), () -> dialogs.openManageDepartment(0)));

        content.clearChildren();
        content.top();
        content.defaults().growX();
        content.add(summaryStrip()).padBottom(Theme.SPACE_4).row();
        Table bar = new Table();
        bar.add(filterChips()).left();
        bar.add(ui.label("A gold star marks an Expert skill. At " + (int) SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD
            + " or more, a skill can be trained toward Expert.", Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG))
            .expandX().right();
        content.add(bar).padBottom(Theme.SPACE_4).row();
        content.add(roster()).expand().fill();
    }

    private Table summaryStrip() {
        AstronautCorps.Summary summary = AstronautCorps.summary(session.state(), session.catalog());
        Table strip = ui.card();
        strip.pad(Theme.BORDER_EMPHASIS);
        String[][] stats = {
            {"Headcount", summary.headcount() + " / " + summary.cap(), null},
            {"On missions / PR tour", summary.onMission() + " / " + summary.onPrTour(),
                summary.onPrTour() == 0 ? null : "+" + summary.prPerSeason() + " prestige / season"},
            {"Idle / in training", summary.idle() + " / " + summary.inTraining(), null},
            {"Experts", String.valueOf(summary.experts()), null},
            {"Near retirement", String.valueOf(summary.nearRetirement()), "retire at " + summary.retirementAge()},
            {"Average morale", summary.headcount() == 0 ? "–" : String.valueOf(summary.averageMorale()), null},
            {"Total salary / season", String.format(Locale.ROOT, "$ %,.1f", summary.totalSalary()), null}};
        for (int i = 0; i < stats.length; i++) {
            Table stat = new Table();
            stat.left();
            stat.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
            stat.defaults().left();
            Table head = new Table();
            head.add(ui.label(stats[i][0].toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            if (i == 0) {
                head.add(new HistoryNote(ui, HEADCOUNT_NOTE)).size(HistoryNote.ICON_SIZE).padLeft(4f);
            }
            stat.add(head).padBottom(2f).row();
            stat.add(ui.label(stats[i][1], Theme.TextStyle.HEADING_SM, Theme.INK)).row();
            if (stats[i][2] != null) {
                stat.add(ui.label(stats[i][2], Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            }
            strip.add(stat).expandX().fillX().uniformX().top();
            strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
        }
        strip.add(buildingCell(Building.ASTRONAUT_CENTER, "the Astronaut Complex", "headcount cap")).top();
        return strip;
    }

    private Table filterChips() {
        Table chips = new Table();
        for (MissionControl.Filter option : MissionControl.Filter.values()) {
            boolean active = option == filter;
            Table chip = new Table();
            chip.setBackground(active ? ui.solid(Theme.SURFACE_CARD)
                : ui.rect(new Color(0f, 0f, 0f, 0f), Theme.ON_PAGE, Theme.BORDER_THIN));
            chip.pad(5f, 12f, 5f, 12f);
            chip.add(ui.label(option.label(), Theme.TextStyle.LABEL, active ? Theme.NAVY : Theme.ON_PAGE_STRONG));
            chip.setTouchable(Touchable.enabled);
            chip.addListener(clicked(() -> {
                filter = option;
                rebuild();
            }));
            chips.add(chip).padRight(6f);
        }
        return chips;
    }

    private Table roster() {
        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        List<Astronaut> rows = session.state().getAstronauts().stream()
            .filter(astronaut -> AstronautCorps.matches(astronaut, filter)).toList();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        if (rows.isEmpty()) {
            String text = session.state().getAstronauts().isEmpty()
                ? "No astronauts yet. Hire your first to fly a crewed mission or sit at CAPCOM."
                : "Nobody matches this filter.";
            list.add(ui.paragraph(text, Theme.TextStyle.BODY, Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (Astronaut astronaut : rows) {
            list.add(row(astronaut)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        card.add(scrollPane(list)).expand().fill();
        return card;
    }

    private Table row(Astronaut astronaut) {
        Table row = new Table();
        row.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        row.add(RosterCells.avatar(ui, astronaut)).size(48f).padRight(Theme.SPACE_4);
        row.add(RosterCells.who(ui, astronaut, "AGE " + astronaut.getAge(), () -> game.setScreen(
            new PersonScreen(game, astronaut, () -> new AstronautComplexScreen(game))))).width(170f).left()
            .padRight(Theme.SPACE_4);

        String text = AstronautCorps.stateText(astronaut, session.state(), session.catalog());
        boolean active = astronaut.getUnavailableSeasons() == 0
            && astronaut.getAssignmentState() != AssignmentState.IDLE;
        Table stateHolder = new Table();
        stateHolder.left();
        stateHolder.defaults().left();
        stateHolder.add(active ? ui.badge(Theme.BadgeKind.ACTIVE, text) : RosterCells.pill(ui, text)).row();
        String hint = AstronautCorps.expertHint(astronaut);
        if (hint != null) {
            stateHolder.add(ui.label(hint, Theme.TextStyle.CAPTION, Theme.GOLD_TEXT)).padTop(4f);
        }
        row.add(stateHolder).width(330f).left().padRight(Theme.SPACE_4);
        row.add(RosterCells.traits(ui, astronaut)).width(RosterCells.TRAITS_WIDTH).left().padRight(Theme.SPACE_4);

        Table skills = new Table();
        for (AstronautSkill skill : AstronautSkill.values()) {
            skills.add(RosterCells.skillCell(ui, CrewSkills.icon(skill.name()), astronaut.getSkill(skill),
                astronaut.getExpertSkills().contains(skill), Tips.skill(CrewSkills.label(skill.name()), skill.name())))
                .width(68f).left().padRight(Theme.SPACE_2);
        }
        row.add(skills).expandX().left();

        Table chips = new Table();
        chips.defaults().left().growX();
        chips.add(RosterCells.statChip(ui, "funds", String.format(Locale.ROOT, "%.1f", astronaut.getSalary()),
            Theme.INK)).padBottom(4f).row();
        chips.add(RosterCells.statChip(ui, "morale", String.valueOf(astronaut.getMorale()),
            RosterCells.moraleColor(astronaut.getMorale())));
        row.add(chips).width(96f).padRight(Theme.SPACE_4);

        Table actions = new Table();
        if (astronaut.getUnavailableSeasons() == 0 && astronaut.getAssignmentState() == AssignmentState.IDLE) {
            actions.add(actionButton("Train", Theme.ButtonKind.SECONDARY, () -> dialogs.openTraining(astronaut)))
                .padRight(6f);
            actions.add(tip(actionButton("PR duty", Theme.ButtonKind.SECONDARY, () -> openPrDuty(astronaut)),
                Tips.prDuty())).padRight(6f);
        } else if (astronaut.getAssignmentState() == AssignmentState.PR_DUTY) {
            actions.add(actionButton("Recall", Theme.ButtonKind.SECONDARY, () -> recall(astronaut))).padRight(6f);
        } else if (astronaut.getAssignmentState() == AssignmentState.IN_TRAINING
            && astronaut.getTrainingKind() != TrainingKind.BASIC) {
            // Basic training can't be cut short (PullOutOfTrainingOutcome.BASIC_TRAINING).
            actions.add(actionButton("Pull out", Theme.ButtonKind.SECONDARY, () -> dialogs.confirmPullOut(astronaut)))
                .padRight(6f);
        } else if (astronaut.getAssignmentState() == AssignmentState.ASSIGNED) {
            MissionControl.Post post = MissionControl.post(astronaut.getId(), session.state(), session.catalog());
            boolean scheduled = post == null || post.mission().getCurrentSeasonIndex() == 0;
            actions.add(actionButton("Mission ›", Theme.ButtonKind.SECONDARY,
                () -> game.setScreen(new MissionsScreen(game, scheduled)))).padRight(6f);
        }
        actions.add(actionButton("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> dialogs.confirmDismiss(astronaut)));
        row.add(actions).width(330f).right();
        return row;
    }

    /**
     * Opens the PR duty dialog for an idle astronaut, as their PR duty button does: what the tour
     * would earn, how diminishing returns and rust work, and a history note.
     *
     * @param astronaut the astronaut
     */
    public void openPrDuty(Astronaut astronaut) {
        AstronautCorps.PrProjection projection = AstronautCorps.prProjection(astronaut, session.state());
        Table card = dialogCard("Astronaut Complex", "PR duty · " + astronaut.getName());
        Table intro = new Table();
        intro.add(ui.paragraph("Interviews, tours and public appearances instead of training. The astronaut stays "
            + "busy, so morale rises, and earns prestige every season until you recall them. On tour they can't "
            + "fly or sit at CAPCOM.", Theme.TextStyle.BODY, Theme.INK_MUTED)).expandX().fillX().padRight(6f);
        intro.add(new HistoryNote(ui, session.state().getArchetype() == Archetype.SOVIET ? HistoryNotes.PR_SOVIET
            : HistoryNotes.PR_NASA)).size(HistoryNote.ICON_SIZE).top();
        card.add(intro).padBottom(Theme.SPACE_4).row();
        Table lines = new Table();
        lines.defaults().growX();
        lines.add(prLine("Leadership " + Math.round(astronaut.getSkill(AstronautSkill.LEADERSHIP)),
            "+" + projection.leadership())).row();
        lines.add(prLine(astronaut.getFlightsFlown() + (astronaut.getFlightsFlown() == 1 ? " flight" : " flights")
            + ", " + astronaut.getFirstsFlown() + " with a first in the game", "+" + projection.fame())).row();
        if (projection.leadership() + projection.fame() > PrDuty.MAX_YIELD) {
            lines.add(prLine("Capped at", String.valueOf(PrDuty.MAX_YIELD))).row();
        }
        lines.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_EMPHASIS).padTop(4f).padBottom(4f).row();
        lines.add(prLine("Earns on tour", "+" + projection.earns() + " prestige / season")).row();
        lines.add(prLine("Agency PR income", "+" + projection.before() + " → +" + projection.after()
            + " / season")).row();
        card.add(lines).padBottom(Theme.SPACE_4).row();
        if (projection.others() > 0) {
            card.add(ui.notice(Theme.NoticeKind.INFO, "The public's attention is limited.", projection.others()
                + (projection.others() == 1 ? " astronaut is" : " astronauts are") + " already on tour. Ranked by "
                + "what they bring, the first earns all of it, the second 60 %, the third 30 %, and any more "
                + "nothing.")).padBottom(Theme.SPACE_3).row();
        }
        card.add(ui.notice(Theme.NoticeKind.WARNING, "Out of practice.", "After " + PrDuty.RUST_FREE_SEASONS
            + " seasons in a row on tour, Piloting and Fitness each drop by " + Math.round(PrDuty.RUST_PER_SEASON)
            + " a season.")).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Send on tour", Theme.ButtonKind.PRIMARY, () ->
            submit(new StartPrDutyAction(astronaut.getId()), result -> {
                closeDialog();
                toast(astronaut.getName() + " is on a PR tour.", Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card, 600f);
    }

    private Table prLine(String label, String value) {
        Table line = new Table();
        line.add(ui.label(label, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
        line.add(ui.label(value, Theme.TextStyle.BODY_STRONG, Theme.INK)).right();
        return line;
    }

    private void recall(Astronaut astronaut) {
        submit(new RecallFromPrDutyAction(astronaut.getId()), result ->
            toast(astronaut.getName() + " is back from the PR tour.", Theme.NoticeKind.SUCCESS));
    }

    /**
     * Opens the training dialog for an astronaut, as their Train button does.
     *
     * @param astronaut the astronaut
     */
    public void openTraining(Astronaut astronaut) {
        dialogs.openTraining(astronaut);
    }

    /**
     * Opens the training dialog with a kind and skill already chosen, as after the player's clicks.
     *
     * @param astronaut the astronaut
     * @param kind      the training kind
     * @param skill     the skill's name
     */
    public void openTraining(Astronaut astronaut, de.mkoehler.launchwindow.model.TrainingKind kind, String skill) {
        dialogs.openTraining(astronaut, kind, skill);
    }

    /**
     * Asks to pull an astronaut out of training, as their Pull out button does.
     *
     * @param astronaut the astronaut
     */
    public void confirmPullOut(Astronaut astronaut) {
        dialogs.confirmPullOut(astronaut);
    }

    /**
     * Asks to dismiss an astronaut, as their Dismiss button does.
     *
     * @param astronaut the astronaut
     */
    public void confirmDismiss(Astronaut astronaut) {
        dialogs.confirmDismiss(astronaut);
    }
}
