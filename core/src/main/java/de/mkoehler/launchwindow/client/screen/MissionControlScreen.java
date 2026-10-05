package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * Mission Control's flight-controller overview ({@code ui-spec.md} &sect;4.7.1, component 11): the
 * department summary, the missions at a glance, filter chips, and one row per controller with
 * their mission and console, all five skills with Expert marks, the Flight Director average, and
 * Train, Pull out, Mission and Dismiss. Hire, Manage department, and the scheduled and active
 * missions are its sub-screens.
 *
 * @author Mario Koehler
 */
public final class MissionControlScreen extends ChromeScreen {

    private final PersonnelDialogs dialogs = new PersonnelDialogs(this, Department.MISSION_CONTROL);
    private MissionControl.Filter filter = MissionControl.Filter.ALL;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public MissionControlScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.MISSION_CONTROL), "Mission Control", "Flight controllers");
        rebuild();
    }

    @Override
    protected void goMissionControl() {
        // Already here.
    }

    @Override
    protected void rebuild() {
        PlayerSnapshot state = session.state();
        titleActions.clearChildren();
        titleActions.add(onClick(ui.pageButton("Hire"), () -> game.setScreen(new HireScreen(game,
            Department.MISSION_CONTROL)))).padRight(10f);
        titleActions.add(onClick(ui.pageButton("Manage department"), () -> dialogs.openManageDepartment(0)))
            .padRight(10f);
        titleActions.add(onClick(ui.pageButton("Scheduled missions · " + MissionControl.scheduled(state).size()),
            () -> game.setScreen(new MissionsScreen(game, true)))).padRight(10f);
        titleActions.add(actionButton("Active missions · " + MissionControl.active(state).size(),
            Theme.ButtonKind.PRIMARY, () -> game.setScreen(new MissionsScreen(game, false))));

        content.clearChildren();
        content.top();
        content.defaults().growX();
        content.add(summaryStrip()).padBottom(Theme.SPACE_4).row();
        Table glance = glance();
        if (glance != null) {
            content.add(glance).padBottom(Theme.SPACE_4).row();
        }
        Table bar = new Table();
        bar.add(filterChips()).left();
        bar.add(ui.label("A gold star marks an Expert skill. FD is the average of all five, what a FLIGHT console reads.",
            Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG)).expandX().right();
        content.add(bar).padBottom(Theme.SPACE_4).row();
        content.add(roster()).expand().fill();
    }

    private Table summaryStrip() {
        MissionControl.Summary summary = MissionControl.summary(session.state(), session.catalog());
        Table strip = ui.card();
        strip.pad(Theme.BORDER_EMPHASIS);
        // The building cell at the strip's end shows the level; this says what the next one holds.
        String next = summary.nextCap() < 0 ? null : "Level " + (summary.level() + 1) + " holds " + summary.nextCap();
        String[][] stats = {
            {"Headcount", summary.headcount() + " / " + summary.cap(), next},
            {"On missions", String.valueOf(summary.onMission()), null},
            {"Idle / in training", summary.idle() + " / " + summary.inTraining(), null},
            {"Experts", String.valueOf(summary.experts()), null},
            {"Average morale", summary.headcount() == 0 ? "–" : String.valueOf(summary.averageMorale()), null},
            {"Total salary / season", String.format(Locale.ROOT, "$ %,.1f", summary.totalSalary()), null}};
        for (int i = 0; i < stats.length; i++) {
            Table stat = new Table();
            stat.left();
            stat.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
            stat.defaults().left();
            stat.add(ui.label(stats[i][0].toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padBottom(2f).row();
            stat.add(ui.label(stats[i][1], Theme.TextStyle.HEADING_SM, Theme.INK)).row();
            if (stats[i][2] != null) {
                stat.add(ui.label(stats[i][2], Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            }
            strip.add(stat).expandX().fillX().uniformX().top();
            strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
        }
        strip.add(buildingCell(Building.MISSION_CONTROL, "Mission Control", "headcount cap")).top();
        return strip;
    }

    /**
     * The missions at a glance: the first scheduled and the first active mission, each linking to
     * its list. {@code null} with neither.
     */
    private Table glance() {
        PlayerSnapshot state = session.state();
        List<InProgressMission> scheduled = MissionControl.scheduled(state);
        List<InProgressMission> active = MissionControl.active(state);
        if (scheduled.isEmpty() && active.isEmpty()) {
            return null;
        }
        Table row = new Table();
        if (!scheduled.isEmpty()) {
            row.add(missionLink(scheduled.get(0), true)).expandX().fillX().uniformX().padRight(Theme.SPACE_4);
        }
        if (!active.isEmpty()) {
            row.add(missionLink(active.get(0), false)).expandX().fillX().uniformX();
        }
        return row;
    }

    private Table missionLink(InProgressMission mission, boolean scheduled) {
        MissionConfiguration configuration = session.catalog().missionConfiguration(mission.getMissionConfigurationId())
            .orElseThrow();
        Table link = ui.card();
        Drawable art = ui.croppedPicture("configurations/" + configuration.id() + ".jpg", 1.6f);
        link.add(new Image(art == null ? ui.solid(Theme.NAVY) : art)).size(96f, 60f).padRight(Theme.SPACE_4);
        Table text = new Table();
        text.left();
        text.defaults().left();
        String eyebrow = scheduled ? "Scheduled · flies at the next turn"
            : "In flight · season " + (mission.getCurrentSeasonIndex() + 1) + " of " + configuration.seasons().size()
                + " next";
        text.add(ui.label(eyebrow.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        text.add(ui.label(MissionControl.missionName(configuration), Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(2f).row();
        text.add(ui.label(MissionsScreen.peopleLine(mission, configuration, state()), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED));
        link.add(text).expandX().fillX().left();
        link.add(ui.label(scheduled ? "Scrub or re-staff ›" : "Watch or re-staff ›", Theme.TextStyle.LABEL,
            Theme.NAVY)).right();
        link.setTouchable(Touchable.enabled);
        link.addListener(clicked(() -> game.setScreen(new MissionsScreen(game, scheduled))));
        return link;
    }

    private PlayerSnapshot state() {
        return session.state();
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
        List<MissionControlOfficer> rows = session.state().getMissionControlOfficers().stream()
            .filter(officer -> MissionControl.matches(officer, filter)).toList();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        if (rows.isEmpty()) {
            String text = session.state().getMissionControlOfficers().isEmpty()
                ? "No flight controllers yet. Hire your first to staff a mission's consoles."
                : "Nobody matches this filter.";
            list.add(ui.paragraph(text, Theme.TextStyle.BODY, Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (MissionControlOfficer officer : rows) {
            list.add(row(officer)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        card.add(scrollPane(list)).expand().fill();
        return card;
    }

    private Table row(MissionControlOfficer officer) {
        Table row = new Table();
        row.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        row.add(RosterCells.avatar(ui, officer)).size(48f).padRight(Theme.SPACE_4);
        Table who = new Table();
        who.left();
        who.defaults().left();
        who.add(ui.label(officer.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
        who.add(ui.label("AGE " + officer.getAge(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(who).width(170f).left().padRight(Theme.SPACE_4);

        String text = MissionControl.stateText(officer, session.state(), session.catalog());
        boolean active = officer.getUnavailableSeasons() == 0 && officer.getAssignmentState() != AssignmentState.IDLE;
        Table stateHolder = new Table();
        stateHolder.left();
        stateHolder.add(active ? ui.badge(Theme.BadgeKind.ACTIVE, text) : RosterCells.pill(ui, text));
        row.add(stateHolder).width(300f).left().padRight(Theme.SPACE_4);

        Table skills = new Table();
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            skills.add(RosterCells.skillCell(ui, CrewSkills.icon(skill.name()), officer.getSkill(skill),
                officer.getExpertSkills().contains(skill))).width(64f).left().padRight(Theme.SPACE_2);
        }
        skills.add(RosterCells.skillCell(ui, CrewSkills.icon(StaffingSlot.FLIGHT_DIRECTOR), officer.flightDirectorSkill(), false))
            .width(64f).left();
        row.add(skills).expandX().left();

        Table chips = new Table();
        chips.defaults().left().growX();
        chips.add(RosterCells.statChip(ui, "funds", String.format(Locale.ROOT, "$ %.1f", officer.getSalary()), Theme.INK))
            .padBottom(4f).row();
        chips.add(RosterCells.statChip(ui, "morale", String.valueOf(officer.getMorale()),
            RosterCells.moraleColor(officer.getMorale())));
        row.add(chips).width(96f).padRight(Theme.SPACE_4);

        Table actions = new Table();
        if (officer.getUnavailableSeasons() == 0 && officer.getAssignmentState() == AssignmentState.IDLE) {
            actions.add(actionButton("Train", Theme.ButtonKind.SECONDARY, () -> dialogs.openTraining(officer)))
                .padRight(6f);
        } else if (officer.getAssignmentState() == AssignmentState.IN_TRAINING
            && officer.getTrainingKind() != TrainingKind.BASIC) {
            // Basic training can't be cut short (PullOutOfTrainingOutcome.BASIC_TRAINING).
            actions.add(actionButton("Pull out", Theme.ButtonKind.SECONDARY, () -> dialogs.confirmPullOut(officer)))
                .padRight(6f);
        } else if (officer.getAssignmentState() == AssignmentState.ASSIGNED) {
            MissionControl.Post post = MissionControl.post(officer.getId(), session.state(), session.catalog());
            boolean scheduled = post == null || post.mission().getCurrentSeasonIndex() == 0;
            actions.add(actionButton("Mission ›", Theme.ButtonKind.SECONDARY,
                () -> game.setScreen(new MissionsScreen(game, scheduled)))).padRight(6f);
        }
        actions.add(actionButton("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> dialogs.confirmDismiss(officer)));
        row.add(actions).right();
        return row;
    }

    /**
     * Opens the training dialog for a controller, as their Train button does.
     *
     * @param officer the controller
     */
    public void openTraining(MissionControlOfficer officer) {
        dialogs.openTraining(officer);
    }

    /**
     * Opens the Manage department dialog, as its button does.
     */
    public void openManageDepartment() {
        dialogs.openManageDepartment(0);
    }
}
