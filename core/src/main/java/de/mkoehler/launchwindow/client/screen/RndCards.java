package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.PayloadRnd;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.ResearchPlanning;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * R&amp;D cards (component 10), shared by the R&amp;D boards and the mission planning screen's
 * Payload R&amp;D tab: a component's reliability bar and four slots, with assign and release going
 * through the host screen's submit loop. Read-only cards show the slots without the actions.
 *
 * @author Mario Koehler
 */
final class RndCards {

    private final ChromeScreen host;
    private final UiKit ui;
    private final String title;
    private final boolean editable;
    private boolean autoAssigning;

    /**
     * Creates the cards for a screen.
     *
     * @param host     the screen they sit on
     * @param title    the assign dialog's eyebrow, such as "Payload R&amp;D"
     * @param editable whether slots can be assigned and released
     */
    RndCards(ChromeScreen host, String title, boolean editable) {
        this.host = host;
        this.ui = host.ui;
        this.title = title;
        this.editable = editable;
    }

    /**
     * Builds a component's card: its reliability bar and four R&amp;D slots.
     *
     * @param card the component's card
     * @return the card
     */
    Table card(PayloadRnd.Card card) {
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
        } else if (editable) {
            TextButton auto = button("Auto-assign", Theme.ButtonKind.SECONDARY, () -> autoAssign(card));
            ui.tooltip(auto, "Auto-assign", "Seats the four best engineers for this component, best in the "
                + "richest slot: those already on it and anyone idle. Engineers busy elsewhere stay where they are.");
            head.add(auto).right();
        }
        box.add(head).padBottom(Theme.SPACE_3).row();

        double current = card.owned().getCurrentReliability();
        double gain = ResearchPlanning.expectedSeasonGain(host.session.state(), host.session.catalog(),
            component.id());
        box.add(new ReliabilityBar(ui, current, component.maxRdReliability(), component.maxReliability(),
            card.owned().getFloorReliability()).projected(current + gain)).height(14f).padBottom(Theme.SPACE_1).row();
        String caption = String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f)", current, component.maxRdReliability());
        if (gain >= 0.05) {
            caption += String.format(Locale.ROOT, " · about +%.1f next season", gain);
        }
        Label captionLabel = ui.label(caption, Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        if (gain >= 0.05) {
            ui.tooltip(captionLabel, "Expected R&D gain", "The lighter part of the bar: what this team adds in an "
                + "average season. Each slot's draw is random, and gains shrink as reliability nears the R&D cap.");
        }
        box.add(captionLabel).padBottom(Theme.SPACE_3).row();
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
            if (!editable) {
                return box;
            }
            com.badlogic.gdx.scenes.scene2d.ui.Label release =
                ui.label("Release", Theme.TextStyle.CAPTION, Theme.DANGER);
            release.setTouchable(Touchable.enabled);
            release.addListener(ChromeScreen.clicked(() -> host.submit(new ReleaseResearchAction(sitting.getId()),
                result -> host.toast("Released " + sitting.getName() + " from " + ContentNames.of(card.component().id())
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
        if (editable && slot == nextFree) {
            com.badlogic.gdx.scenes.scene2d.ui.Label assign = ui.label("+ Assign", Theme.TextStyle.BODY_SM, Theme.NAVY);
            assign.setTouchable(Touchable.enabled);
            assign.addListener(ChromeScreen.clicked(() -> openAssign(card)));
            box.add(assign).padTop(4f);
        }
        return box;
    }

    /**
     * Opens the assign dialog for a component: the idle engineers, best in its skill first.
     *
     * @param card the component's card
     */
    void openAssign(PayloadRnd.Card card) {
        MissionComponent component = card.component();
        if (card.nextFreeSlot() < 0) {
            host.toast(card.atCap() ? "R&D can't improve this further." : "All four R&D slots are taken.",
                Theme.NoticeKind.INFO);
            return;
        }
        List<SetSpecialist> candidates = PayloadRnd.assignable(host.session.state(), component);
        if (candidates.isEmpty()) {
            host.toast("No idle engineers. Hire more, or free someone up.", Theme.NoticeKind.INFO);
            return;
        }
        Table dialogCard = host.dialogCard(title, "Assign to " + ContentNames.of(component.id()));
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
            row.add(button("Assign", Theme.ButtonKind.PRIMARY, () -> host.submit(
                new AssignResearchAction(specialist.getId(), component.id()), result -> {
                    host.closeDialog();
                    host.toast("Assigned " + specialist.getName() + " to " + ContentNames.of(component.id()) + ".",
                        Theme.NoticeKind.SUCCESS);
                })));
            list.add(row).padBottom(Theme.SPACE_2).row();
        }
        dialogCard.add(list).padBottom(Theme.SPACE_3).row();
        dialogCard.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).right().fill(false, false);
        host.showDialog(dialogCard);
    }

    /**
     * Seats the best team on a component ({@link ResearchPlanning#bestTeam}): releases whoever sits in
     * the wrong slot or isn't in the team, then assigns the rest in order, each taking the lowest free
     * slot, so the best engineer ends up in the richest slot. One action after another, like hiring.
     *
     * @param card the component's card
     */
    void autoAssign(PayloadRnd.Card card) {
        if (autoAssigning) {
            return;
        }
        MissionComponent component = card.component();
        List<SetSpecialist> team = ResearchPlanning.bestTeam(host.session.state(), component);
        if (team.isEmpty()) {
            host.toast("No idle engineers. Hire more, or free someone up.", Theme.NoticeKind.INFO);
            return;
        }
        List<PlayerAction> actions = PayloadRnd.autoAssignActions(card, team);
        if (actions.isEmpty()) {
            host.toast("Already the best team available.", Theme.NoticeKind.INFO);
            return;
        }
        autoAssigning = true;
        runChain(new ArrayDeque<>(actions), ContentNames.of(component.id()), team.size());
    }

    private void runChain(Deque<PlayerAction> queue, String name, int teamSize) {
        if (queue.isEmpty()) {
            autoAssigning = false;
            host.stateChanged();
            host.toast("Seated the best " + (teamSize == 1 ? "engineer" : teamSize + " engineers") + " on " + name
                + ".", Theme.NoticeKind.SUCCESS);
            return;
        }
        host.game.gateway().submit(host.session.credentials(), queue.poll(), result -> {
            if (!host.isShowing()) {
                autoAssigning = false;
                return;
            }
            if (result.kind() == ServerResult.Kind.OK) {
                host.session.update(result);
                runChain(queue, name, teamSize);
            } else {
                // Stop at the first problem; the screen shows whatever was done.
                autoAssigning = false;
                host.handle(result, null);
            }
        });
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
