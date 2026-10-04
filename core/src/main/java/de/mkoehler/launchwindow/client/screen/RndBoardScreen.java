package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.PayloadRnd;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.List;
import java.util.Locale;

/**
 * An R&amp;D board (component 10): the SET Centre's Payload R&amp;D ({@code ui-spec.md} &sect;4.8.3)
 * or the VAB's Rocket R&amp;D (&sect;4.5.3), which work the same way. One card per owned, active
 * component with its reliability bar and four R&amp;D slots. Assign an idle engineer (they take
 * the lowest free slot) or release one; a component at its R&amp;D ceiling shows "Cap reached" and
 * its slots released.
 *
 * @author Mario Koehler
 */
public final class RndBoardScreen extends ChromeScreen {

    /**
     * Which board: what it holds, where it sits, and what it says when empty.
     */
    public enum Board {
        /** Payload components, in the SET Centre. */
        PAYLOAD("SET Centre", "Payload R&D", "No payload components yet. Open a program in Headquarters to get its "
            + "satellites, probes and capsules; they show up here for R&D."),
        /** Rockets, in the VAB. */
        ROCKET("VAB", "Rocket R&D", "No rockets yet. Open a rocket program in the VAB; its rocket shows up here "
            + "for R&D.");

        private final String building;
        private final String title;
        private final String empty;

        Board(String building, String title, String empty) {
            this.building = building;
            this.title = title;
            this.empty = empty;
        }

        private List<Crumb> crumbs() {
            return this == PAYLOAD
                ? List.of(Crumb.SPACE_COMPLEX, Crumb.SET_CENTRE, new Crumb(title, null))
                : List.of(Crumb.SPACE_COMPLEX, Crumb.VAB, Crumb.ROCKET_PROGRAMS, new Crumb(title, null));
        }
    }

    private final Board board;

    /**
     * Builds the screen from the session.
     *
     * @param game  the game, in a session
     * @param board which board
     */
    public RndBoardScreen(LaunchWindowGame game, Board board) {
        super(game, board.crumbs(), board.building, board.title);
        this.board = board;
        rebuild();
    }

    @Override
    protected void goBack() {
        if (board == Board.PAYLOAD) {
            goSetCentre();
        } else {
            game.setScreen(new RocketProgramsScreen(game));
        }
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        List<PayloadRnd.Card> cards = cards();
        if (cards.isEmpty()) {
            Table empty = ui.card();
            empty.add(ui.paragraph(board.empty, Theme.TextStyle.BODY, Theme.INK_MUTED)).growX();
            content.add(empty).growX();
            return;
        }
        Table grid = new Table();
        grid.top();
        for (int i = 0; i < cards.size(); i++) {
            grid.add(card(cards.get(i))).expandX().fillX().uniformX().top().pad(0f, 0f, Theme.SPACE_4,
                i % 2 == 0 ? Theme.SPACE_4 : 0f);
            if (i % 2 == 1) {
                grid.row();
            }
        }
        if (cards.size() % 2 == 1) {
            grid.add().expandX().fillX().uniformX();
        }
        ScrollPane scroll = scrollPane(grid);
        content.add(scroll).expand().fill();
    }

    /**
     * Returns the board's cards.
     *
     * @return the cards
     */
    public List<PayloadRnd.Card> cards() {
        return PayloadRnd.cards(session.state(), session.catalog(), board == Board.ROCKET);
    }

    private Table card(PayloadRnd.Card card) {
        MissionComponent component = card.component();
        Table box = ui.card();
        box.defaults().growX().left();

        Table head = new Table();
        String icon = SetSkills.icon(component.associatedSkill());
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        head.add(glyph).height(18f).width(18f * Math.min(1f, ui.iconAspect(icon))).padRight(Theme.SPACE_2);
        head.add(ui.label(ContentNames.of(component.id()), Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        if (card.atCap()) {
            head.add(ui.badge(Theme.BadgeKind.SUCCESS, "Cap reached")).right();
        }
        box.add(head).padBottom(Theme.SPACE_3).row();

        double current = card.owned().getCurrentReliability();
        box.add(new ReliabilityBar(ui, current, component.maxRdReliability(), component.maxReliability(),
            card.owned().getFloorReliability())).height(14f).padBottom(Theme.SPACE_1).row();
        box.add(ui.label(String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f)", current, component.maxRdReliability()),
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        if (card.atCap()) {
            box.add(ui.paragraph("Released automatically when the R&D ceiling was reached.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        }

        Table slots = new Table();
        int next = card.nextFreeSlot();
        for (int slot = 0; slot < PayloadRnd.SLOTS; slot++) {
            slots.add(slot(card, slot, next)).expandX().fillX().uniformX().height(64f)
                .padRight(slot < PayloadRnd.SLOTS - 1 ? Theme.SPACE_2 : 0f);
        }
        box.add(slots);
        return box;
    }

    private Table slot(PayloadRnd.Card card, int slot, int nextFree) {
        SetSpecialist sitting = card.slots().get(slot);
        Table box = new Table();
        box.pad(4f);
        if (sitting != null) {
            box.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f), Theme.NAVY,
                Theme.BORDER_THIN));
            box.add(ui.label(sitting.getName(), Theme.TextStyle.BODY_SM, Theme.INK)).row();
            box.add(ui.label("Slot " + (slot + 1), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
            com.badlogic.gdx.scenes.scene2d.ui.Label release =
                ui.label("Release", Theme.TextStyle.CAPTION, Theme.DANGER);
            release.setTouchable(Touchable.enabled);
            release.addListener(clicked(() -> submit(new ReleaseResearchAction(sitting.getId()),
                result -> toast("Released " + sitting.getName() + " from " + ContentNames.of(card.component().id())
                    + ".", Theme.NoticeKind.SUCCESS))));
            box.add(release);
            return box;
        }
        if (card.atCap()) {
            box.setBackground(ui.rect(Theme.SKILL_TRACK, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            box.add(ui.label("Slot " + (slot + 1) + " · empty", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
            box.add(ui.label("Released", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            return box;
        }
        box.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        box.add(ui.label("Slot " + (slot + 1) + " · empty", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        if (slot == nextFree) {
            com.badlogic.gdx.scenes.scene2d.ui.Label assign = ui.label("+ Assign", Theme.TextStyle.BODY_SM, Theme.NAVY);
            assign.setTouchable(Touchable.enabled);
            assign.addListener(clicked(() -> openAssign(card)));
            box.add(assign).padTop(4f);
        }
        return box;
    }

    /**
     * Opens the assign dialog for a component: the idle engineers, best in its skill first.
     *
     * @param card the component's card
     */
    public void openAssign(PayloadRnd.Card card) {
        MissionComponent component = card.component();
        if (card.nextFreeSlot() < 0) {
            toast(card.atCap() ? "R&D can't improve this further." : "All four R&D slots are taken.",
                Theme.NoticeKind.INFO);
            return;
        }
        List<SetSpecialist> candidates = PayloadRnd.assignable(session.state(), component);
        if (candidates.isEmpty()) {
            toast("No idle engineers. Hire more, or free someone up.", Theme.NoticeKind.INFO);
            return;
        }
        Table dialogCard = dialogCard(board.title, "Assign to " + ContentNames.of(component.id()));
        dialogCard.add(ui.paragraph("They take slot " + (card.nextFreeSlot() + 1) + ". Sorted by "
            + SetSkills.label(component.associatedSkill()) + ", the skill this component needs.",
            Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        Table list = new Table();
        list.defaults().growX();
        for (SetSpecialist specialist : candidates.subList(0, Math.min(6, candidates.size()))) {
            Table row = new Table();
            row.add(ui.label(specialist.getName(), Theme.TextStyle.BODY, Theme.INK)).expandX().left();
            row.add(ui.label(SetSkills.label(component.associatedSkill()) + " "
                    + Math.round(SetDepartment.skill(specialist, component.associatedSkill())),
                Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padRight(Theme.SPACE_3);
            row.add(button("Assign", Theme.ButtonKind.PRIMARY, () -> submit(
                new AssignResearchAction(specialist.getId(), component.id()), result -> {
                    closeDialog();
                    toast("Assigned " + specialist.getName() + " to " + ContentNames.of(component.id()) + ".",
                        Theme.NoticeKind.SUCCESS);
                })));
            list.add(row).padBottom(Theme.SPACE_2).row();
        }
        dialogCard.add(list).padBottom(Theme.SPACE_3).row();
        dialogCard.add(button("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).right().fill(false, false);
        showDialog(dialogCard);
    }

    private TextButton button(String text, Theme.ButtonKind kind, Runnable action) {
        TextButton button = ui.button(text, kind);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }
}
