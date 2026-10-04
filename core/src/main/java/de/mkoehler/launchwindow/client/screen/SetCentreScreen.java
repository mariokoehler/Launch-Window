package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.AdjustSalariesAction;
import de.mkoehler.launchwindow.actions.DismissAction;
import de.mkoehler.launchwindow.actions.PullOutOfTrainingAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.DepartmentSalaries;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TrainingProgress;

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
            () -> game.setScreen(new PayloadRndScreen(game))));
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
            strip.add(stat).expandX().fillX().uniformX();
            if (i < stats.length - 1) {
                strip.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).fillY();
            }
        }
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

    // ------------------------------------------------------------------ training dialog

    /**
     * Opens the training dialog (component 12) for an engineer, or says they're busy.
     *
     * @param specialist the engineer
     */
    public void openTraining(SetSpecialist specialist) {
        if (SetDepartment.isBusy(specialist)) {
            toast("They're busy.", Theme.NoticeKind.INFO);
            return;
        }
        new TrainingDialog(specialist).show();
    }

    /**
     * Opens the training dialog with a kind and skill already chosen, as after the player's clicks.
     *
     * @param specialist the engineer
     * @param kind       the training kind
     * @param skill      the skill
     */
    public void openTraining(SetSpecialist specialist, TrainingKind kind, SetSkill skill) {
        new TrainingDialog(specialist).choose(kind, skill).show();
    }

    /**
     * The training dialog's state and widgets: kind, skill, and for Permanent the target level.
     */
    final class TrainingDialog {
        private final SetSpecialist specialist;
        private TrainingKind kind = TrainingKind.BASIC;
        private SetSkill skill;
        private int target;

        TrainingDialog(SetSpecialist specialist) {
            this.specialist = specialist;
        }

        void show() {
            showDialog(build());
        }

        TrainingDialog choose(TrainingKind chosenKind, SetSkill chosenSkill) {
            kind = chosenKind;
            skill = chosenSkill;
            clampTarget();
            return this;
        }

        /** Keeps the target within what the server accepts, defaulting to 90 (the mockup's default). */
        private void clampTarget() {
            int min = minimumTarget();
            if (target < min) {
                target = Math.max(min, 90);
            }
            target = Math.min(target, (int) SkillGrowth.SKILL_CAP);
        }

        /** The lowest target the server accepts: above the current skill (TARGET_NOT_ABOVE_SKILL). */
        private int minimumTarget() {
            double current = skill == null ? 0 : SetDepartment.skill(specialist, skill);
            return Math.min((int) SkillGrowth.SKILL_CAP, (int) Math.floor(current) + 1);
        }

        private Table build() {
            Table card = dialogCard("Training", "Start training — " + specialist.getName());
            card.add(ui.label("KIND", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
            Table segments = new Table();
            segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY, Theme.BORDER_THIN));
            segments.pad(Theme.BORDER_THIN);
            int basic = TrainingProgress.SET_BASIC_TRAINING_SEASONS;
            int advanced = TrainingProgress.ADVANCED_TRAINING_SEASONS;
            segments.add(segment("Basic · " + seasons(basic), TrainingKind.BASIC)).expandX().fillX().uniformX();
            segments.add(segment("Advanced · " + seasons(advanced), TrainingKind.ADVANCED)).expandX().fillX()
                .uniformX();
            segments.add(segment("Permanent", TrainingKind.PERMANENT)).expandX().fillX().uniformX();
            card.add(segments).padBottom(Theme.SPACE_4).row();

            card.add(ui.label("SKILL", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
            Table picks = new Table();
            picks.left();
            for (SetSkill option : SetSkill.values()) {
                picks.add(skillPick(option)).size(64f, 52f).padRight(Theme.SPACE_2);
            }
            card.add(picks).padBottom(Theme.SPACE_4).row();

            if (kind == TrainingKind.PERMANENT) {
                Table head = new Table();
                head.add(ui.label("TARGET LEVEL", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX().left();
                Label value = ui.label(String.valueOf(target), Theme.TextStyle.BODY_SM, Theme.INK);
                head.add(value).right();
                card.add(head).padBottom(Theme.SPACE_2).row();
                Slider slider = new Slider(minimumTarget(), (float) SkillGrowth.SKILL_CAP, 1f, false, ui.slider());
                slider.setValue(target);
                slider.setDisabled(skill == null);
                slider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        target = Math.round(slider.getValue());
                        value.setText(String.valueOf(target));
                    }
                });
                card.add(slider).padBottom(Theme.SPACE_4).row();
            }

            Table buttons = new Table();
            buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, SetCentreScreen.this::closeDialog))
                .padRight(10f);
            TextButton start = button("Start Training", Theme.ButtonKind.PRIMARY, this::confirm);
            start.setDisabled(skill == null);
            buttons.add(start);
            card.add(buttons).right().fill(false, false);
            return card;
        }

        private String seasons(int count) {
            return count + (count == 1 ? " season" : " seasons");
        }

        private Table segment(String text, TrainingKind segmentKind) {
            boolean active = segmentKind == kind;
            Table segment = new Table();
            if (active) {
                segment.setBackground(ui.solid(Theme.NAVY));
            }
            segment.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
            segment.add(ui.label(text, Theme.TextStyle.LABEL, active ? Theme.ON_FILL : Theme.NAVY));
            segment.setTouchable(Touchable.enabled);
            segment.addListener(clicked(() -> {
                choose(segmentKind, skill);
                show();
            }));
            return segment;
        }

        private Table skillPick(SetSkill option) {
            boolean active = option == skill;
            Table pick = new Table();
            pick.setBackground(active
                ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.1f), Theme.GOLD, Theme.BORDER_EMPHASIS)
                : ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            Image glyph = new Image(ui.icon(SetSkills.icon(option)), Scaling.fit);
            glyph.setColor(Theme.NAVY);
            pick.add(glyph).height(18f).width(18f * Math.min(1f, ui.iconAspect(SetSkills.icon(option)))).padBottom(4f)
                .row();
            pick.add(ui.label(SetSkills.shortLabel(option), Theme.TextStyle.CAPTION, Theme.NAVY));
            pick.setTouchable(Touchable.enabled);
            pick.addListener(clicked(() -> {
                choose(kind, option);
                show();
            }));
            return pick;
        }

        private void confirm() {
            if (skill == null) {
                return;
            }
            double level = kind == TrainingKind.PERMANENT ? target : 0;
            String text = "Started training — " + specialist.getName() + " ("
                + kind.name().charAt(0) + kind.name().substring(1).toLowerCase(Locale.ROOT) + ", "
                + SetSkills.label(skill) + ").";
            submit(new StartTrainingAction(PersonnelGroup.SET, specialist.getId(), kind, skill.name(), level),
                result -> {
                    closeDialog();
                    toast(text, Theme.NoticeKind.SUCCESS);
                });
        }
    }

    // ------------------------------------------------------------------ confirmations

    /**
     * Asks to pull an engineer out of training (component 5).
     *
     * @param specialist the engineer
     */
    public void confirmPullOut(SetSpecialist specialist) {
        Table card = dialogCard("Confirm", "Pull " + specialist.getName() + " out of training?");
        card.add(ui.paragraph("They keep partial credit for the round in progress.", Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(confirmButtons("Pull Out", Theme.ButtonKind.PRIMARY, () -> submit(
            new PullOutOfTrainingAction(PersonnelGroup.SET, specialist.getId()), result -> {
                closeDialog();
                toast("Pulled " + specialist.getName() + " out of training — partial credit applied.",
                    Theme.NoticeKind.SUCCESS);
            }))).right().fill(false, false);
        showDialog(card);
    }

    /**
     * Asks to dismiss an engineer (component 5).
     *
     * @param specialist the engineer
     */
    public void confirmDismiss(SetSpecialist specialist) {
        Table card = dialogCard("Confirm", "Dismiss " + specialist.getName() + "?");
        String body = specialist.getAssignmentState() == AssignmentState.ASSIGNED
            ? "This also frees their R&D slot on " + ContentNames.of(specialist.getRdTargetComponentId())
                + ". Dismissal is free, but final."
            : "Dismissal is free, but final.";
        card.add(ui.paragraph(body, Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(confirmButtons("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> submit(
            new DismissAction(PersonnelGroup.SET, specialist.getId()), result -> {
                closeDialog();
                toast("Dismissed " + specialist.getName() + ".", Theme.NoticeKind.SUCCESS);
            }))).right().fill(false, false);
        showDialog(card);
    }

    private Table confirmButtons(String label, Theme.ButtonKind kind, Runnable action) {
        Table buttons = new Table();
        buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(button(label, kind, action));
        return buttons;
    }

    // ------------------------------------------------------------------ manage department

    /**
     * Opens the Manage department dialog (component 13): a one-off change to every SET salary by
     * -50 to +50 percent, previewing the department's salary and average morale afterwards.
     */
    public void openManageDepartment() {
        openManageDepartment(0);
    }

    /**
     * Opens the Manage department dialog with the slider at a given percentage.
     *
     * @param percent the starting percentage
     */
    public void openManageDepartment(int percent) {
        List<SetSpecialist> roster = session.state().getSetSpecialists();
        if (roster.isEmpty()) {
            toast("There's nobody in the department yet.", Theme.NoticeKind.INFO);
            return;
        }
        Table card = dialogCard("SET Centre", "Manage department");
        card.add(ui.paragraph("A one-off adjustment to every SET salary. Morale moves with it — up for a raise, "
            + "down three times as much for a cut.", Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4)
            .row();
        Table head = new Table();
        head.add(ui.label("SALARY CHANGE", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX().left();
        Label value = ui.label(percentLabel(percent), Theme.TextStyle.BODY_SM, Theme.INK);
        head.add(value).right();
        card.add(head).padBottom(Theme.SPACE_2).row();
        Slider slider = new Slider(DepartmentSalaries.MIN_PERCENT, DepartmentSalaries.MAX_PERCENT, 1f, false,
            ui.slider());
        slider.setValue(percent);
        card.add(slider).padBottom(Theme.SPACE_4).row();

        Table preview = new Table();
        preview.setBackground(ui.cardAccent(Theme.NAVY));
        preview.pad(Theme.SPACE_4, Theme.SPACE_4 + Theme.BORDER_ACCENT, Theme.SPACE_4, Theme.SPACE_4);
        Label salary = ui.label("", Theme.TextStyle.HEADING_SM, Theme.INK);
        Label morale = ui.label("", Theme.TextStyle.HEADING_SM, Theme.INK);
        Table left = new Table();
        left.left();
        left.defaults().left();
        left.add(ui.label("PROJECTED SALARY / SEASON", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        left.add(salary);
        Table right = new Table();
        right.left();
        right.defaults().left();
        right.add(ui.label("PROJECTED AVERAGE MORALE", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        right.add(morale);
        preview.add(left).expandX().left();
        preview.add(right).expandX().left();
        card.add(preview).padBottom(Theme.SPACE_4).row();

        Table buttons = new Table();
        buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        TextButton ok = button("OK", Theme.ButtonKind.PRIMARY, () -> {
            int chosen = Math.round(slider.getValue());
            submit(new AdjustSalariesAction(PersonnelGroup.SET, chosen), result -> {
                closeDialog();
                toast("Adjusted SET salaries by " + percentLabel(chosen) + ".", Theme.NoticeKind.SUCCESS);
            });
        });
        buttons.add(ok);
        card.add(buttons).right().fill(false, false);

        Runnable update = () -> {
            int chosen = Math.round(slider.getValue());
            value.setText(percentLabel(chosen));
            salary.setText(String.format(Locale.ROOT, "$ %,.1f", DepartmentSalaries.projectedSalary(roster, chosen)));
            morale.setText(String.valueOf(DepartmentSalaries.projectedAverageMorale(roster, chosen)));
            ok.setDisabled(chosen == 0);
        };
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                update.run();
            }
        });
        update.run();
        showDialog(card);
    }

    private static String percentLabel(int percent) {
        return (percent > 0 ? "+" : "") + percent + "%";
    }
}
