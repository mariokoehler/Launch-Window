package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.SkillBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hire SET personnel ({@code ui-spec.md} &sect;4.8.2, component 14): the group's candidate pool
 * (it refreshes every season), sortable by skill; select candidates into the Recruits panel, then
 * Hire Recruits. Built from the older Personnel Recruitment mockup, with the SET Centre mockup's
 * newer header and footer (no End Season button; the breadcrumb starts at Space Complex).
 *
 * <p>Selection can't go past the SET Centre's free roster places. Hiring sends one hire per recruit,
 * in order, and sums the outcomes up in one toast: a candidate who has left the pool, or a roster
 * that filled up meanwhile, is reported, not lost silently.</p>
 *
 * @author Mario Koehler
 */
public final class HireScreen extends ChromeScreen {

    private static final float LIST_WIDTH = 1180f;

    private final Set<String> selected = new LinkedHashSet<>();
    private SetSkill sortBy = SetSkill.ROCKETS;
    private boolean descending = true;
    private boolean hiring;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public HireScreen(LaunchWindowGame game) {
        super(game, List.of(new Crumb("Space Complex", null), new Crumb("SET Centre", null),
            new Crumb("Personnel Recruitment", null)), "SET Centre", "Personnel Recruitment");
        rebuild();
    }

    @Override
    protected void goBack() {
        goSetCentre();
    }

    /**
     * Selects or unselects a candidate, as their Select button does.
     *
     * @param candidateId the candidate's id
     */
    public void toggle(String candidateId) {
        if (selected.remove(candidateId)) {
            rebuild();
            return;
        }
        if (selected.size() >= freePlaces()) {
            toast(Refusals.text("ROSTER_FULL"), Theme.NoticeKind.INFO);
            return;
        }
        selected.add(candidateId);
        rebuild();
    }

    private int freePlaces() {
        return Math.max(0, SetDepartment.cap(session.state(), session.catalog())
            - session.state().getSetSpecialists().size());
    }

    @Override
    protected void rebuild() {
        List<SetSpecialist> pool = session.state().getSetSpecialistCandidates();
        selected.removeIf(id -> pool.stream().noneMatch(candidate -> candidate.getId().equals(id)));
        content.clearChildren();
        content.top();

        Table main = new Table();
        main.add(candidatesPanel(pool)).width(LIST_WIDTH).expandY().fillY().top().padRight(Theme.SPACE_5);
        main.add(recruitsPanel(pool)).expand().fill().top();
        content.add(main).expand().fill().row();
        content.add(actionsRow(pool)).growX().padTop(Theme.SPACE_5);
    }

    private Table candidatesPanel(List<SetSpecialist> pool) {
        Table panel = new Table();
        panel.top();
        Table head = new Table();
        head.add(ui.label("Available candidates · Sorted by " + SetSkills.label(sortBy) + " "
            + (descending ? "↓" : "↑"), Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).expandX().left();
        head.add(sortButton(descending ? "sort_desc" : "sort_asc", true, () -> {
            descending = !descending;
            rebuild();
        })).size(32f).padRight(Theme.SPACE_1);
        head.add(new Image(ui.solid(Theme.ON_PAGE))).width(Theme.BORDER_THIN).height(32f).padRight(Theme.SPACE_1);
        for (SetSkill skill : SetSkill.values()) {
            head.add(sortButton(SetSkills.icon(skill), skill == sortBy, () -> {
                sortBy = skill;
                rebuild();
            })).size(32f).padLeft(Theme.SPACE_1);
        }
        panel.add(head).growX().padBottom(Theme.SPACE_2).row();

        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        Comparator<SetSpecialist> bySkill = Comparator.comparingDouble(c -> SetDepartment.skill(c, sortBy));
        List<SetSpecialist> sorted = pool.stream().sorted(descending ? bySkill.reversed() : bySkill).toList();
        if (sorted.isEmpty()) {
            list.add(ui.paragraph("No candidates this season. The pool refreshes when the turn resolves.",
                Theme.TextStyle.BODY, Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (SetSpecialist candidate : sorted) {
            list.add(candidateRow(candidate)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        ScrollPane scroll = scrollPane(list);
        card.add(scroll).expand().fill();
        panel.add(card).expand().fill();
        return panel;
    }

    private Table candidateRow(SetSpecialist candidate) {
        boolean isSelected = selected.contains(candidate.getId());
        Table row = new Table();
        if (isSelected) {
            row.setBackground(ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f)));
        }
        Image accent = new Image(ui.solid(isSelected ? Theme.GOLD : new Color(0f, 0f, 0f, 0f)));
        row.add(accent).width(Theme.BORDER_ACCENT).fillY();
        Table inner = new Table();
        inner.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        inner.add(avatar(candidate.getName())).size(56f).padRight(Theme.SPACE_4);

        Table who = new Table();
        who.left();
        who.defaults().left();
        who.add(ui.label(candidate.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
        who.add(ui.label("AGE " + candidate.getAge(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        inner.add(who).width(170f).left().padRight(Theme.SPACE_4);

        Table skills = new Table();
        for (SetSkill skill : SetSkill.values()) {
            double value = SetDepartment.skill(candidate, skill);
            skills.add(ui.label(SetSkills.label(skill), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).width(148f).left()
                .padRight(Theme.SPACE_2);
            skills.add(new SkillBar(ui, value)).expandX().fillX().height(6f).padRight(Theme.SPACE_2);
            Label number = ui.label(String.format(Locale.ROOT, "%.1f%%", value), Theme.TextStyle.BODY_SM, Theme.INK);
            number.setAlignment(Align.right);
            skills.add(number).width(52f).padBottom(3f).row();
        }
        inner.add(skills).expandX().fillX().padRight(Theme.SPACE_4);

        Table chips = new Table();
        chips.defaults().growX().left().padBottom(4f);
        chips.add(statChip("funds", String.format(Locale.ROOT, "$ %.1f", candidate.getSalary()))).row();
        chips.add(statChip("morale", String.valueOf(candidate.getMorale()))).row();
        chips.add(statChip("learning_potential", String.valueOf(candidate.getLearningPotential())));
        inner.add(chips).width(108f).padRight(Theme.SPACE_4);

        TextButton select = button(isSelected ? "Selected" : "Select",
            isSelected ? Theme.ButtonKind.PRIMARY : Theme.ButtonKind.SECONDARY, () -> toggle(candidate.getId()));
        inner.add(select).width(96f);
        row.add(inner).expandX().fillX();
        return row;
    }

    private Table recruitsPanel(List<SetSpecialist> pool) {
        Table panel = new Table();
        panel.top();
        Table head = new Table();
        head.add(ui.label("Recruits", Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).expandX().left();
        head.add(ui.label(selected.size() + " selected · " + freePlaces() + " places free", Theme.TextStyle.CAPTION,
            Theme.ON_PAGE_MUTED));
        panel.add(head).growX().height(32f).padBottom(Theme.SPACE_2).row();

        Table card = ui.card();
        card.top();
        card.defaults().growX();
        List<SetSpecialist> chosen = chosen(pool);
        if (chosen.isEmpty()) {
            Table empty = new Table();
            Image glyph = new Image(ui.icon("set_employees"), Scaling.fit);
            glyph.setColor(Theme.INK_MUTED);
            empty.add(glyph).size(36f).padBottom(Theme.SPACE_2).row();
            Label text = ui.paragraph("No recruits selected yet. Choose specialists from the candidate list to add "
                + "them here.", Theme.TextStyle.BODY, Theme.INK_MUTED);
            text.setAlignment(Align.center);
            empty.add(text).width(320f);
            card.add(empty).expand().center();
        }
        for (SetSpecialist recruit : chosen) {
            Table entry = new Table();
            entry.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.SURFACE_CARD, 0f));
            Image accent = new Image(ui.solid(Theme.GOLD_TEXT));
            entry.add(accent).width(Theme.BORDER_ACCENT).fillY();
            Table inner = new Table();
            inner.pad(Theme.SPACE_3);
            inner.add(avatar(recruit.getName())).size(40f).padRight(Theme.SPACE_3);
            Table who = new Table();
            who.defaults().left();
            who.add(ui.label(recruit.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).row();
            who.add(ui.label(String.format(Locale.ROOT, "AGE %d · $ %.1f", recruit.getAge(), recruit.getSalary()),
                Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            inner.add(who).expandX().left();
            inner.add(button("Remove", Theme.ButtonKind.DESTRUCTIVE, () -> toggle(recruit.getId())));
            entry.add(inner).expandX().fillX();
            card.add(entry).padBottom(Theme.SPACE_3).row();
        }
        panel.add(card).expand().fill();
        return panel;
    }

    private Table actionsRow(List<SetSpecialist> pool) {
        Table row = new Table();
        TextButton hire = button(hiring ? "Hiring…" : "Hire Recruits", Theme.ButtonKind.PRIMARY, this::hire);
        hire.setDisabled(selected.isEmpty() || hiring);
        row.add(hire).padRight(Theme.SPACE_3);
        TextButton cancel = ui.pageButton("Cancel");
        cancel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selected.clear();
                rebuild();
            }
        });
        row.add(cancel);
        row.add().expandX();
        double total = chosen(pool).stream().mapToDouble(SetSpecialist::getSalary).sum();
        row.add(ui.label("Added fixed costs", Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE)).padRight(Theme.SPACE_2);
        row.add(ui.label(String.format(Locale.ROOT, "$ %,.1f / season", total), Theme.TextStyle.HEADING_SM,
            Theme.ON_PAGE_STRONG));
        return row;
    }

    private List<SetSpecialist> chosen(List<SetSpecialist> pool) {
        return pool.stream().filter(candidate -> selected.contains(candidate.getId())).toList();
    }

    /**
     * Hires every selected recruit, one hire after another, then sums the outcomes up in one toast.
     */
    public void hire() {
        if (hiring || selected.isEmpty()) {
            return;
        }
        hiring = true;
        rebuild();
        hireNext(new ArrayDeque<>(selected), 0, new ArrayList<>());
    }

    private void hireNext(Deque<String> queue, int hired, List<String> problems) {
        if (queue.isEmpty()) {
            hiring = false;
            selected.clear();
            refreshChrome();
            rebuild();
            String done = hired == 0 ? "Nobody was hired." : "Hired " + hired + (hired == 1 ? " recruit." : " recruits.");
            toast(problems.isEmpty() ? done : done + " " + String.join(" ", new LinkedHashSet<>(problems)),
                hired > 0 && problems.isEmpty() ? Theme.NoticeKind.SUCCESS : Theme.NoticeKind.INFO);
            if (!problems.isEmpty()) {
                refresh();
            }
            return;
        }
        String candidateId = queue.poll();
        game.gateway().submit(session.credentials(), new HireAction(PersonnelGroup.SET, candidateId), result -> {
            if (!isShowing()) {
                return;
            }
            if (result.kind() == ServerResult.Kind.OK) {
                session.update(result);
                hireNext(queue, hired + 1, problems);
            } else if (result.kind() == ServerResult.Kind.REFUSED) {
                problems.add(Refusals.text(result.message()));
                hireNext(queue, hired, problems);
            } else {
                // The connection is gone: stop here rather than piling up failures.
                problems.add("The connection broke off; try the rest again.");
                queue.clear();
                hireNext(queue, hired, problems);
            }
        });
    }

    private Table sortButton(String icon, boolean active, Runnable action) {
        Table button = new Table();
        button.setBackground(active ? ui.solid(Theme.NAVY) : ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(active ? Theme.ON_FILL : Theme.NAVY);
        button.add(glyph).height(16f).width(16f * Math.min(1f, ui.iconAspect(icon)));
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(action));
        return button;
    }

    private Table statChip(String icon, String text) {
        Table chip = new Table();
        chip.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        chip.pad(3f, Theme.SPACE_2, 3f, Theme.SPACE_2);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.INK);
        chip.add(glyph).size(12f).padRight(Theme.SPACE_1);
        chip.add(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK)).expandX().left();
        return chip;
    }

    private Table avatar(String name) {
        Table avatar = new Table();
        avatar.setBackground(ui.solid(Theme.NAVY));
        Label initials = ui.label(SetCentreScreen.initials(name), Theme.TextStyle.HEADING_SM, Theme.ON_FILL);
        initials.setAlignment(Align.center);
        avatar.add(initials);
        return avatar;
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
}
