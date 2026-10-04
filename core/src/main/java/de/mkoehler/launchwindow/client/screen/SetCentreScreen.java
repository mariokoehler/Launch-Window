package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.List;
import java.util.Locale;

/**
 * The SET Centre's personnel overview ({@code ui-spec.md} &sect;4.8.1, component 11): the
 * department summary strip, filter chips, sort by skill, and one row per engineer with Train,
 * Pull out and Dismiss. Hire, Manage department and Payload R&amp;D are its sub-screens.
 *
 * @author Mario Koehler
 */
public final class SetCentreScreen extends ChromeScreen {

    private static final float SORT_BUTTON = 26f;

    private SetDepartment.Filter filter = SetDepartment.Filter.ALL;
    private SetSkill sortBy = SetSkill.ROCKETS;
    private boolean descending = true;
    private final PersonnelDialogs dialogs = new PersonnelDialogs(this, Department.SET);

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public SetCentreScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.SET_CENTRE), "SET Centre",
            "Personnel overview");
        titleActions.add(onPage(ui.pageButton("Hire"), () -> game.setScreen(new HireScreen(game)))).padRight(10f);
        titleActions.add(onPage(ui.pageButton("Manage Department"), this::openManageDepartment)).padRight(10f);
        titleActions.add(button("Payload R&D", Theme.ButtonKind.PRIMARY,
            () -> game.setScreen(new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD))));
        rebuild();
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        content.add(summaryStrip()).padBottom(Theme.SPACE_4).row();
        content.add(filterAndSortBar()).padBottom(Theme.SPACE_4).row();
        content.add(roster()).expand().fill();
    }

    @Override
    protected void goSetCentre() {
        // Already here.
    }

    private Table summaryStrip() {
        SetDepartment.Summary summary = SetDepartment.summary(session.state(), session.catalog());
        Table strip = ui.card();
        strip.pad(Theme.BORDER_EMPHASIS);
        String[][] stats = {
            {"Headcount", summary.headcount() + " / " + summary.cap()},
            {"In training / Researching", summary.inTraining() + " / " + summary.researching()},
            {"Near retirement", String.valueOf(summary.nearRetirement())},
            {"Average morale", summary.headcount() == 0 ? "–" : String.valueOf(summary.averageMorale())},
            {"Total salary / season", String.format(Locale.ROOT, "$ %,.1f", summary.totalSalary())}};
        for (int i = 0; i < stats.length; i++) {
            Table stat = new Table();
            stat.left();
            stat.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
            stat.defaults().left();
            stat.add(ui.label(stats[i][0].toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padBottom(2f).row();
            stat.add(ui.label(stats[i][1], Theme.TextStyle.HEADING_SM, Theme.INK));
            strip.add(stat).expandX().fillX().uniformX().top();
            strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
        }
        strip.add(buildingCell(Building.SET_CENTER, "the SET Centre", "headcount cap")).top();
        return strip;
    }

    private Table filterAndSortBar() {
        Table bar = new Table();
        Table chips = new Table();
        for (SetDepartment.Filter option : SetDepartment.Filter.values()) {
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
        bar.add(chips).expandX().left();

        Table sort = new Table();
        sort.add(ui.label("Sort by " + SetSkills.label(sortBy) + " " + (descending ? "↓" : "↑"),
            Theme.TextStyle.CAPTION, Theme.ON_PAGE_MUTED)).padRight(Theme.SPACE_2);
        sort.add(sortButton(descending ? "sort_desc" : "sort_asc", true, () -> {
            descending = !descending;
            rebuild();
        })).size(SORT_BUTTON).padRight(Theme.SPACE_1);
        sort.add(new Image(ui.solid(Theme.ON_PAGE))).width(Theme.BORDER_THIN).height(SORT_BUTTON).padRight(Theme.SPACE_1);
        for (SetSkill skill : SetSkill.values()) {
            sort.add(sortButton(SetSkills.icon(skill), skill == sortBy, () -> {
                sortBy = skill;
                rebuild();
            })).size(SORT_BUTTON).padLeft(Theme.SPACE_1);
        }
        bar.add(sort).right();
        return bar;
    }

    private Table sortButton(String icon, boolean active, Runnable action) {
        Table button = new Table();
        button.setBackground(active ? ui.solid(Theme.NAVY) : ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(active ? Theme.ON_FILL : Theme.NAVY);
        float height = 14f;
        button.add(glyph).height(height).width(height * Math.min(1f, ui.iconAspect(icon)));
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(action));
        return button;
    }

    private Actor roster() {
        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        List<SetSpecialist> rows = SetDepartment.rows(session.state().getSetSpecialists(), filter, sortBy, descending);
        Table list = new Table();
        list.top();
        list.defaults().growX();
        if (rows.isEmpty()) {
            String text = session.state().getSetSpecialists().isEmpty()
                ? "No engineers yet. Hire your first SET specialists to get research going."
                : "Nobody matches this filter.";
            list.add(ui.paragraph(text, Theme.TextStyle.BODY, Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (SetSpecialist specialist : rows) {
            list.add(row(specialist)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        ScrollPane scroll = scrollPane(list);
        card.add(scroll).expand().fill();
        return card;
    }

    private Table row(SetSpecialist specialist) {
        Table row = new Table();
        row.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        row.add(avatar(specialist.getName())).size(48f).padRight(Theme.SPACE_4);

        Table who = new Table();
        who.left();
        who.defaults().left();
        who.add(ui.label(specialist.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
        who.add(ui.label("AGE " + specialist.getAge(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(who).width(160f).left().padRight(Theme.SPACE_4);

        Container<Label> state = SetDepartment.isActive(specialist)
            ? ui.badge(Theme.BadgeKind.ACTIVE, SetDepartment.stateText(specialist))
            : pill(SetDepartment.stateText(specialist));
        Table stateHolder = new Table();
        stateHolder.left();
        stateHolder.add(state);
        row.add(stateHolder).width(280f).left().padRight(Theme.SPACE_4);

        Table skills = new Table();
        for (SetSkill skill : SetSkill.values()) {
            Table cell = new Table();
            Image glyph = new Image(ui.icon(SetSkills.icon(skill)), Scaling.fit);
            glyph.setColor(Theme.NAVY);
            cell.add(glyph).height(14f).width(14f * Math.min(1f, ui.iconAspect(SetSkills.icon(skill)))).padRight(4f);
            cell.add(ui.label(String.valueOf(Math.round(SetDepartment.skill(specialist, skill))),
                Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).left();
            skills.add(cell).width(52f).left().padRight(Theme.SPACE_3);
        }
        row.add(skills).expandX().left();

        Table chips = new Table();
        chips.defaults().left().growX();
        chips.add(statChip("funds", String.format(Locale.ROOT, "$ %.1f", specialist.getSalary()), Theme.INK))
            .padBottom(4f).row();
        chips.add(statChip("morale", String.valueOf(specialist.getMorale()), moraleColor(specialist.getMorale())));
        row.add(chips).width(96f).padRight(Theme.SPACE_4);

        Table actions = new Table();
        actions.add(button("Train", Theme.ButtonKind.SECONDARY, () -> openTraining(specialist))).padRight(6f);
        if (specialist.getAssignmentState() == AssignmentState.IN_TRAINING) {
            actions.add(button("Pull out", Theme.ButtonKind.SECONDARY, () -> confirmPullOut(specialist))).padRight(6f);
        }
        actions.add(button("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> confirmDismiss(specialist)));
        row.add(actions).right();
        return row;
    }

    private Container<Label> pill(String text) {
        Container<Label> pill = new Container<>(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        pill.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        pill.pad(Theme.SPACE_1, Theme.SPACE_2, Theme.SPACE_1, Theme.SPACE_2);
        return pill;
    }

    private Table statChip(String icon, String text, Color color) {
        Table chip = new Table();
        chip.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        chip.pad(3f, Theme.SPACE_2, 3f, Theme.SPACE_2);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(color);
        chip.add(glyph).size(12f).padRight(Theme.SPACE_1);
        chip.add(ui.label(text, Theme.TextStyle.CAPTION, color)).expandX().left();
        return chip;
    }

    private static Color moraleColor(int morale) {
        if (morale >= 65) {
            return Theme.SUCCESS;
        }
        return morale >= 45 ? Theme.GOLD_TEXT : Theme.DANGER;
    }

    /**
     * Builds the navy initials square standing in for a portrait.
     *
     * @param name the person's name
     * @return the avatar
     */
    Table avatar(String name) {
        Table avatar = new Table();
        avatar.setBackground(ui.solid(Theme.NAVY));
        Label initials = ui.label(initials(name), Theme.TextStyle.HEADING_SM, Theme.ON_FILL);
        initials.setAlignment(Align.center);
        avatar.add(initials);
        return avatar;
    }

    static String initials(String name) {
        StringBuilder letters = new StringBuilder();
        for (String part : name.trim().split("[\\s.]+")) {
            if (!part.isEmpty() && letters.length() < 2) {
                letters.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return letters.toString();
    }

    private TextButton onPage(TextButton button, Runnable action) {
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

    private TextButton button(String text, Theme.ButtonKind kind, Runnable action) {
        TextButton button = ui.button(text, kind);
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

    // ------------------------------------------------------------------ dialogs

    /**
     * Opens the training dialog (component 12) for an engineer, or says they're busy.
     *
     * @param specialist the engineer
     */
    public void openTraining(SetSpecialist specialist) {
        dialogs.openTraining(specialist);
    }

    /**
     * Opens the training dialog with a kind and skill already chosen, as after the player's clicks.
     *
     * @param specialist the engineer
     * @param kind       the training kind
     * @param skill      the skill
     */
    public void openTraining(SetSpecialist specialist, TrainingKind kind, SetSkill skill) {
        dialogs.openTraining(specialist, kind, skill == null ? null : skill.name());
    }

    /**
     * Asks to pull an engineer out of training (component 5).
     *
     * @param specialist the engineer
     */
    public void confirmPullOut(SetSpecialist specialist) {
        dialogs.confirmPullOut(specialist);
    }

    /**
     * Asks to dismiss an engineer (component 5).
     *
     * @param specialist the engineer
     */
    public void confirmDismiss(SetSpecialist specialist) {
        dialogs.confirmDismiss(specialist);
    }

    /**
     * Opens the Manage department dialog (component 13).
     */
    public void openManageDepartment() {
        dialogs.openManageDepartment(0);
    }

    /**
     * Opens the Manage department dialog with the slider at a given percentage.
     *
     * @param percent the starting percentage
     */
    public void openManageDepartment(int percent) {
        dialogs.openManageDepartment(percent);
    }
}
