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
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.rules.HiringPool;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hire personnel ({@code ui-spec.md} &sect;4.8.2 and &sect;4.7.1, component 14) for a department, the
 * SET Centre's engineers or Mission Control's flight controllers: the group's candidate pool (it
 * refreshes every season), sortable by skill; select candidates into the Recruits panel, then
 * Hire Recruits. Built from the older Personnel Recruitment mockup, with the SET Centre mockup's
 * newer header and footer (no End Season button; the breadcrumb starts at Space Complex).
 *
 * <p>Selection can't go past the building's free roster places. Hiring sends one hire per recruit,
 * in order, and sums the outcomes up in one toast: a candidate who has left the pool, or a roster
 * that filled up meanwhile, is reported, not lost silently.</p>
 *
 * @author Mario Koehler
 */
public final class HireScreen extends ChromeScreen {

    private static final float LIST_WIDTH = 1180f;

    private final Set<String> selected = new LinkedHashSet<>();
    /** The selected recruits who get the one-off signing bonus. */
    private final Set<String> bonuses = new LinkedHashSet<>();
    private final Department department;
    private String sortBy;
    private boolean descending = true;
    private boolean hiring;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public HireScreen(LaunchWindowGame game) {
        this(game, Department.SET);
    }

    /**
     * Builds the screen for a department.
     *
     * @param game       the game, in a session
     * @param department the department hiring
     */
    public HireScreen(LaunchWindowGame game, Department department) {
        super(game, List.of(Crumb.SPACE_COMPLEX, switch (department) {
            case SET -> Crumb.SET_CENTRE;
            case ASTRONAUT -> Crumb.ASTRONAUT_COMPLEX;
            case MISSION_CONTROL -> Crumb.MISSION_CONTROL;
        }, new Crumb("Personnel Recruitment", null)), department.place(),
            "Personnel Recruitment");
        this.department = department;
        this.sortBy = department.skills().get(0);
        rebuild();
    }

    @Override
    protected void goBack() {
        switch (department) {
            case SET -> goSetCentre();
            case ASTRONAUT -> goAstronautComplex();
            case MISSION_CONTROL -> goMissionControl();
        }
    }

    /**
     * Selects or unselects a candidate, as their Select button does.
     *
     * @param candidateId the candidate's id
     */
    public void toggle(String candidateId) {
        if (selected.remove(candidateId)) {
            bonuses.remove(candidateId);
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

    /**
     * Switches the signing bonus on or off for a selected recruit, as its toggle does.
     *
     * @param candidateId the recruit's id
     */
    public void toggleBonus(String candidateId) {
        if (!selected.contains(candidateId)) {
            return;
        }
        if (!bonuses.remove(candidateId)) {
            bonuses.add(candidateId);
        }
        rebuild();
    }

    private int freePlaces() {
        return Math.max(0, department.cap(session.state(), session.catalog())
            - department.roster(session.state()).size());
    }

    @Override
    protected void rebuild() {
        List<? extends Personnel> pool = department.candidates(session.state());
        selected.removeIf(id -> pool.stream().noneMatch(candidate -> candidate.getId().equals(id)));
        content.clearChildren();
        content.top();

        Table main = new Table();
        main.add(candidatesPanel(pool)).width(LIST_WIDTH).expandY().fillY().top().padRight(Theme.SPACE_5);
        main.add(recruitsPanel(pool)).expand().fill().top();
        content.add(main).expand().fill().row();
        content.add(actionsRow(pool)).growX().padTop(Theme.SPACE_5);
    }

    private Table candidatesPanel(List<? extends Personnel> pool) {
        Table panel = new Table();
        panel.top();
        Table head = new Table();
        head.add(ui.label("Available candidates · Sorted by " + department.label(sortBy) + " "
            + (descending ? "↓" : "↑"), Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).expandX().left();
        head.add(sortButton(descending ? "sort_desc" : "sort_asc", true, () -> {
            descending = !descending;
            rebuild();
        })).size(32f).padRight(Theme.SPACE_1);
        head.add(new Image(ui.solid(Theme.ON_PAGE))).width(Theme.BORDER_THIN).height(32f).padRight(Theme.SPACE_1);
        for (String skill : department.ratings()) {
            Table button = sortButton(department.icon(skill), skill.equals(sortBy), () -> {
                sortBy = skill;
                rebuild();
            });
            ui.tooltip(button, "Sort by " + department.label(skill), null);
            head.add(button).size(32f).padLeft(Theme.SPACE_1);
        }
        panel.add(head).growX().padBottom(Theme.SPACE_2).row();

        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        Comparator<Personnel> bySkill = Comparator.comparingDouble(c -> department.skill(c, sortBy));
        List<? extends Personnel> sorted = pool.stream().sorted(descending ? bySkill.reversed() : bySkill).toList();
        if (sorted.isEmpty()) {
            list.add(ui.paragraph("No candidates this season. The pool refreshes when the turn resolves.",
                Theme.TextStyle.BODY, Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (Personnel candidate : sorted) {
            list.add(candidateRow(candidate)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        ScrollPane scroll = scrollPane(list);
        card.add(scroll).expand().fill();
        panel.add(card).expand().fill();
        return panel;
    }

    private Table candidateRow(Personnel candidate) {
        boolean isSelected = selected.contains(candidate.getId());
        Table row = new Table();
        if (isSelected) {
            row.setBackground(ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f)));
        }
        Image accent = new Image(ui.solid(isSelected ? Theme.GOLD : new Color(0f, 0f, 0f, 0f)));
        row.add(accent).width(Theme.BORDER_ACCENT).fillY();
        Table inner = new Table();
        inner.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        inner.add(RosterCells.avatar(ui, candidate)).size(56f).top().padRight(Theme.SPACE_4);

        Table who = new Table();
        who.left();
        who.defaults().left();
        who.add(ui.label(candidate.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
        who.add(ui.label(department.ageLine(candidate), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        // One hint from a former colleague (People canvas, 2026-10-07); the traits stay hidden.
        String hint = People.hintQuote(candidate.getHint());
        if (hint != null) {
            Table quote = new Table();
            quote.left();
            quote.defaults().left();
            quote.add(ui.label("A former colleague:", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
            quote.add(ui.label(hint, Theme.TextStyle.TAGLINE, Theme.INK));
            ui.tooltip(quote, "A colleague's hint", "A word from someone who worked with this candidate before. A "
                + "telling hint is true to one of the candidate's traits, but every hint fits several; a plain one "
                + "proves nothing.");
            who.add(quote).padTop(Theme.SPACE_2).padLeft(Theme.SPACE_2);
        }
        inner.add(who).width(190f).left().top().padRight(Theme.SPACE_4);

        Table skills = new Table();
        for (String skill : department.ratings()) {
            double value = department.skill(candidate, skill);
            // The Flight Director value (controllers only) is the average of the five above it, set in bold.
            boolean average = StaffingSlot.FLIGHT_DIRECTOR.equals(skill);
            Label name = ui.label(average ? "Flight Director (avg.)" : department.label(skill),
                average ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY_SM, average ? Theme.INK : Theme.INK_MUTED);
            Tips.Tip tip = Tips.skill(department.label(skill), skill);
            ui.tooltip(name, tip.title(), tip.text());
            skills.add(name).width(148f).left().padRight(Theme.SPACE_2);
            skills.add(new SkillBar(ui, value)).expandX().fillX().height(6f).padRight(Theme.SPACE_2);
            Label number = ui.label(String.format(Locale.ROOT, "%.1f%%", value), Theme.TextStyle.BODY_SM, Theme.INK);
            number.setAlignment(Align.right);
            skills.add(number).width(52f).padBottom(3f).row();
        }
        inner.add(skills).expandX().fillX().padRight(Theme.SPACE_4);

        Table chips = new Table();
        chips.defaults().growX().left().padBottom(4f);
        chips.add(statChip("funds", String.format(Locale.ROOT, "%.1f", candidate.getSalary()))).row();
        chips.add(statChip("morale", String.valueOf(candidate.getMorale()))).row();
        chips.add(statChip("learning_potential", String.valueOf(candidate.getLearningPotential())));
        inner.add(chips).width(108f).padRight(Theme.SPACE_4);

        TextButton select = button(isSelected ? "Selected" : "Select",
            isSelected ? Theme.ButtonKind.PRIMARY : Theme.ButtonKind.SECONDARY, () -> toggle(candidate.getId()));
        inner.add(select).width(96f);
        row.add(inner).expandX().fillX();
        return row;
    }

    private Table recruitsPanel(List<? extends Personnel> pool) {
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
        List<? extends Personnel> chosen = chosen(pool);
        if (chosen.isEmpty()) {
            Table empty = new Table();
            Image glyph = new Image(ui.icon(department.icon()), Scaling.fit);
            glyph.setColor(Theme.INK_MUTED);
            empty.add(glyph).size(36f).padBottom(Theme.SPACE_2).row();
            Label text = ui.paragraph("No recruits selected yet. Choose " + department.people() + " from the "
                + "candidate list to add them here.", Theme.TextStyle.BODY, Theme.INK_MUTED);
            text.setAlignment(Align.center);
            empty.add(text).width(320f);
            card.add(empty).expand().center();
        }
        for (Personnel recruit : chosen) {
            Table entry = new Table();
            entry.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.SURFACE_CARD, 0f));
            Image accent = new Image(ui.solid(Theme.GOLD_TEXT));
            entry.add(accent).width(Theme.BORDER_ACCENT).fillY();
            Table inner = new Table();
            inner.pad(Theme.SPACE_3);
            inner.add(RosterCells.avatar(ui, recruit)).size(40f).padRight(Theme.SPACE_3);
            Table who = new Table();
            who.defaults().left();
            who.add(ui.label(recruit.getName(), Theme.TextStyle.HEADING_SM, Theme.INK)).row();
            String hint = People.hintQuote(recruit.getHint());
            who.add(ui.label(String.format(Locale.ROOT, "AGE %d · $ %.1f", recruit.getAge(), recruit.getSalary())
                + (hint == null ? "" : " · a former colleague: " + hint), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            inner.add(who).expandX().left();
            inner.add(button("Remove", Theme.ButtonKind.DESTRUCTIVE, () -> toggle(recruit.getId()))).row();
            inner.add(bonusToggle(recruit)).colspan(3).left().padTop(Theme.SPACE_2);
            entry.add(inner).expandX().fillX();
            card.add(entry).padBottom(Theme.SPACE_3).row();
        }
        panel.add(card).expand().fill();
        return panel;
    }

    private Table actionsRow(List<? extends Personnel> pool) {
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
        row.add(cancel).padRight(Theme.SPACE_4);
        // Hiring costs time as well as money: nobody can be used before basic training ends.
        int basic = department.basicSeasons();
        row.add(ui.label("New hires start with " + basic + (basic == 1 ? " season" : " seasons")
            + " of basic training.", Theme.TextStyle.BODY_SM, Theme.ON_PAGE));
        row.add().expandX();
        long bonusTotal = chosen(pool).stream().filter(recruit -> bonuses.contains(recruit.getId()))
            .mapToLong(HiringPool::signingBonusCost).sum();
        if (bonusTotal > 0) {
            row.add(ui.label("Signing bonuses", Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE)).padRight(Theme.SPACE_2);
            row.add(ui.label(String.format(Locale.ROOT, "$ %,d once", bonusTotal), Theme.TextStyle.HEADING_SM,
                Theme.ON_PAGE_STRONG)).padRight(Theme.SPACE_5);
        }
        double total = chosen(pool).stream().mapToDouble(Personnel::getSalary).sum();
        row.add(ui.label("Added fixed costs", Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE)).padRight(Theme.SPACE_2);
        row.add(ui.label(String.format(Locale.ROOT, "$ %,.1f / season", total), Theme.TextStyle.HEADING_SM,
            Theme.ON_PAGE_STRONG));
        return row;
    }

    private List<? extends Personnel> chosen(List<? extends Personnel> pool) {
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
            bonuses.clear();
            refreshChrome();
            rebuild();
            int basic = department.basicSeasons();
            String done = hired == 0 ? "Nobody was hired." : "Hired " + hired + (hired == 1 ? " recruit" : " recruits")
                + ", off to " + basic + (basic == 1 ? " season" : " seasons") + " of basic training.";
            toast(problems.isEmpty() ? done : done + " " + String.join(" ", new LinkedHashSet<>(problems)),
                hired > 0 && problems.isEmpty() ? Theme.NoticeKind.SUCCESS : Theme.NoticeKind.INFO);
            if (!problems.isEmpty()) {
                refresh();
            }
            return;
        }
        String candidateId = queue.poll();
        HireAction action = new HireAction(department.group(), candidateId, bonuses.contains(candidateId));
        game.gateway().submit(session.credentials(), action, result -> {
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

    /**
     * The signing-bonus toggle under a recruit: a check box with its cost and the morale it gives.
     */
    private Table bonusToggle(Personnel recruit) {
        boolean on = bonuses.contains(recruit.getId());
        Table toggle = new Table();
        Table box = new Table();
        box.setBackground(on ? ui.solid(Theme.NAVY) : ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        if (on) {
            Image check = new Image(ui.icon("check"), Scaling.fit);
            check.setColor(Theme.ON_FILL);
            box.add(check).size(12f);
        }
        toggle.add(box).size(18f).padRight(Theme.SPACE_2);
        toggle.add(ui.label(String.format(Locale.ROOT, "Signing bonus · $ %,d once · morale %d → %d",
            HiringPool.signingBonusCost(recruit), recruit.getMorale(),
            HiringPool.moraleWithSigningBonus(recruit.getMorale())), Theme.TextStyle.BODY_SM, Theme.INK));
        toggle.setTouchable(Touchable.enabled);
        toggle.addListener(clicked(() -> toggleBonus(recruit.getId())));
        tip(toggle, Tips.signingBonus());
        return toggle;
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
        return RosterCells.statChip(ui, icon, text, Theme.INK);
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
