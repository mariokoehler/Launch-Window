package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.AstronautCorps;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.model.AssignmentState;
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
            {"On missions", String.valueOf(summary.onMission()), null},
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
        Table who = new Table();
        who.left();
        who.defaults().left();
        who.add(ui.label(astronaut.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
        who.add(ui.label("AGE " + astronaut.getAge(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(who).width(170f).left().padRight(Theme.SPACE_4);

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

        Table skills = new Table();
        for (AstronautSkill skill : AstronautSkill.values()) {
            skills.add(RosterCells.skillCell(ui, CrewSkills.icon(skill.name()), astronaut.getSkill(skill),
                astronaut.getExpertSkills().contains(skill))).width(64f).left().padRight(Theme.SPACE_2);
        }
        row.add(skills).expandX().left();

        Table chips = new Table();
        chips.defaults().left().growX();
        chips.add(RosterCells.statChip(ui, "funds", String.format(Locale.ROOT, "$ %.1f", astronaut.getSalary()),
            Theme.INK)).padBottom(4f).row();
        chips.add(RosterCells.statChip(ui, "morale", String.valueOf(astronaut.getMorale()),
            RosterCells.moraleColor(astronaut.getMorale())));
        row.add(chips).width(96f).padRight(Theme.SPACE_4);

        Table actions = new Table();
        if (astronaut.getUnavailableSeasons() == 0 && astronaut.getAssignmentState() == AssignmentState.IDLE) {
            actions.add(actionButton("Train", Theme.ButtonKind.SECONDARY, () -> dialogs.openTraining(astronaut)))
                .padRight(6f);
        } else if (astronaut.getAssignmentState() == AssignmentState.IN_TRAINING) {
            actions.add(actionButton("Pull out", Theme.ButtonKind.SECONDARY, () -> dialogs.confirmPullOut(astronaut)))
                .padRight(6f);
        } else if (astronaut.getAssignmentState() == AssignmentState.ASSIGNED) {
            MissionControl.Post post = MissionControl.post(astronaut.getId(), session.state(), session.catalog());
            boolean scheduled = post == null || post.mission().getCurrentSeasonIndex() == 0;
            actions.add(actionButton("Mission ›", Theme.ButtonKind.SECONDARY,
                () -> game.setScreen(new MissionsScreen(game, scheduled)))).padRight(6f);
        }
        actions.add(actionButton("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> dialogs.confirmDismiss(astronaut)));
        row.add(actions).width(250f).right();
        return row;
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
