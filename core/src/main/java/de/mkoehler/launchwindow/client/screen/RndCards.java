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
import de.mkoehler.launchwindow.client.ui.ChemistryIndicator;
import de.mkoehler.launchwindow.client.ui.MilestoneLabels;
import de.mkoehler.launchwindow.client.ui.RelationshipMarker;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.TraitChip;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Outlook;
import de.mkoehler.launchwindow.client.view.PayloadRnd;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Chemistry;
import de.mkoehler.launchwindow.rules.Personality;
import de.mkoehler.launchwindow.rules.ResearchPlanning;
import de.mkoehler.launchwindow.rules.TraitEffects;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * R&amp;D cards (component 10), shared by the R&amp;D boards and the mission planning screen's
 * Payload R&amp;D tab: a component's reliability bar and four slots, with assign and release going
 * through the host screen's submit loop. Read-only cards show the slots without the actions. Each
 * card shows its team's chemistry as the player knows it (component 21), and a revealed
 * Perfectionist's raised cap as a second, dashed marker (People canvas, 2026-10-07).
 *
 * @author Mario Koehler
 */
final class RndCards {

    /** The assign dialog's width: wide enough for traits, marks and the chemistry preview. */
    private static final float ASSIGN_DIALOG_WIDTH = 1220f;

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
        ResearchPlanning.Projection projection = ResearchPlanning.project(host.session.state(),
            host.session.catalog(), component.id());
        double gain = projection.moving() ? projection.reliability()[0] - current : 0;
        // The cap the player is told: raised while a revealed Perfectionist is on the team.
        double cap = TraitEffects.effectiveCap(host.session.state(), component, List.of(), true);
        ReliabilityBar bar = new ReliabilityBar(ui, current, component.maxRdReliability(), component.maxReliability(),
            card.owned().getFloorReliability()).projected(current + gain).raisedCap(cap);
        // Milestones ahead with the seasons to each (the user's request, 2026-10-10).
        MilestoneLabels labels = milestones(host, component, projection, bar);
        if (labels != null) {
            box.add(labels).padBottom(2f).row();
        }
        box.add(bar).height(14f).padBottom(Theme.SPACE_1).row();
        String caption = String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f)", current, component.maxRdReliability());
        if (cap > component.maxRdReliability()) {
            List<String> perfectionists = team(card).stream()
                .filter(engineer -> Personality.known(engineer, Trait.PERFECTIONIST))
                .map(engineer -> People.surname(engineer.getName())).toList();
            boolean one = perfectionists.size() == 1;
            caption = String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f, raised to %.1f while %s, %s, %s on the team)",
                current, component.maxRdReliability(), cap, String.join(" and ", perfectionists),
                one ? "a perfectionist" : "perfectionists", one ? "is" : "are");
        }
        if (gain >= 0.05) {
            caption += String.format(Locale.ROOT, " · about +%.1f next season", gain);
        }
        Label captionLabel = ui.label(caption, Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        if (gain >= 0.05) {
            ui.tooltip(captionLabel, "Expected R&D gain", "The lighter part of the bar: what this team adds in an "
                + "average season. Each slot's draw is random, and gains shrink as reliability nears the R&D cap. "
                + "The numbers above the bar are the seasons to each mark.");
        }
        String leaving = Outlook.departureLine(projection);
        box.add(captionLabel).padBottom(leaving == null ? Theme.SPACE_3 : 2f).row();
        if (leaving != null) {
            box.add(ui.paragraph(leaving, Theme.TextStyle.CAPTION, Theme.GOLD_TEXT)).padBottom(Theme.SPACE_3).row();
        }
        List<SetSpecialist> team = team(card);
        // Past the cap the player knows of while people still sit there: a hidden Perfectionist's doing.
        if (card.atCap() && team.isEmpty()) {
            box.add(ui.paragraph("Released automatically when the R&D ceiling was reached.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        }
        if (!team.isEmpty()) {
            box.add(chemistryRow(component, team)).padBottom(Theme.SPACE_3).row();
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

    /**
     * The milestone tooltip: each mark with its average seasons, the season it lands by and the
     * spread of the rules' random draws.
     */
    /**
     * Marks a component's bar with the milestones still ahead (component 25) and returns the row of
     * season counts to sit above it, with its tooltip; {@code null} when nobody works on it or the
     * cap is reached. Shared by the R&amp;D cards and the turn report's R&amp;D section.
     *
     * @param host       the screen
     * @param component  the component
     * @param projection its projection with the team on it now
     * @param bar        its bar, which gets the ticks
     * @return the labels, or {@code null}
     */
    static MilestoneLabels milestones(ChromeScreen host, MissionComponent component,
                                      ResearchPlanning.Projection projection, ReliabilityBar bar) {
        List<Outlook.Marker> markers = projection.moving() ? Outlook.markers(projection) : List.of();
        bar.milestones(markers.stream().filter(marker -> !marker.cap()).mapToDouble(Outlook.Marker::value).toArray());
        if (markers.isEmpty()) {
            return null;
        }
        MilestoneLabels labels = new MilestoneLabels(bar.max());
        for (Outlook.Marker marker : markers) {
            labels.add(host.ui, marker.value(), Outlook.approx(marker.seasons()), marker.cap() ? Theme.GOLD_TEXT
                : Theme.NAVY, marker.cap());
        }
        host.ui.tooltip(labels, "Seasons to each mark", outlook(host, component, markers));
        return labels;
    }

    private static String outlook(ChromeScreen host, MissionComponent component, List<Outlook.Marker> markers) {
        double[] targets = markers.stream().mapToDouble(Outlook.Marker::value).toArray();
        ResearchPlanning.Range[] ranges = ResearchPlanning.ranges(host.session.state(), host.session.catalog(),
            component.id(), targets);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < markers.size(); i++) {
            Outlook.Marker marker = markers.get(i);
            String mark = marker.cap() ? String.format(Locale.ROOT, "R&D cap %.1f %%", marker.value())
                : String.format(Locale.ROOT, "%.0f %%", marker.value());
            if (marker.seasons().isEmpty()) {
                text.append(mark).append(": not within ten years with this team.\n");
                continue;
            }
            int seasons = marker.seasons().getAsInt();
            text.append(mark).append(": about ").append(Outlook.seasons(seasons)).append(", by ")
                .append(Outlook.by(host.session.openTurn(), seasons)).append(" (")
                .append(Outlook.range(ranges[i])).append(" in 8 runs of 10).\n");
        }
        text.append("With the team on it now, as the rules average it: skills grow, retirements leave, nobody "
            + "joins. A random event's R&D boost or cut counts while it lasts.");
        return text.toString();
    }

    /**
     * Who sits in each slot, with a candidate in the next free one.
     */
    private static SetSpecialist[] seats(PayloadRnd.Card card, SetSpecialist candidate) {
        SetSpecialist[] seats = card.slots().toArray(new SetSpecialist[0]);
        if (candidate != null && card.nextFreeSlot() >= 0) {
            seats[card.nextFreeSlot()] = candidate;
        }
        return seats;
    }

    /** The cap a team works towards: raised by a revealed Perfectionist on it. */
    private double cap(MissionComponent component, PayloadRnd.Card card, SetSpecialist candidate) {
        List<SetSpecialist> members = java.util.Arrays.stream(seats(card, candidate))
            .filter(java.util.Objects::nonNull).toList();
        return Math.min(component.maxReliability(), component.maxRdReliability() + TraitEffects.capBonus(members,
            true));
    }

    /**
     * The assign dialog's projection cell: seasons to the cap with the team now, then with the
     * candidate seated, "~6 \u2192 ~4".
     */
    private Label toCap(PayloadRnd.Card card, SetSpecialist candidate, java.util.OptionalInt now) {
        MissionComponent component = card.component();
        java.util.OptionalInt then = ResearchPlanning.project(host.session.state(), component,
            card.owned().getCurrentReliability(), seats(card, candidate)).seasonsTo(cap(component, card, candidate));
        String text = team(card).isEmpty() ? Outlook.approx(then) : Outlook.approx(now) + " \u2192 " + Outlook.approx(then);
        boolean faster = then.isPresent() && (now.isEmpty() || then.getAsInt() < now.getAsInt());
        Label label = ui.label(text, Theme.TextStyle.BODY_SM, faster ? Theme.SUCCESS : Theme.INK);
        ui.tooltip(label, "Seasons to the R&D cap", (team(card).isEmpty() ? "" : "With the team now, then with "
            + People.surname(candidate.getName()) + " in slot " + (card.nextFreeSlot() + 1) + ". ")
            + "The rules' average: skills grow, retirements leave, nobody else joins.");
        return label;
    }

    private static List<SetSpecialist> team(PayloadRnd.Card card) {
        return card.slots().stream().filter(java.util.Objects::nonNull).toList();
    }

    /**
     * Builds a card's team chemistry row: the indicator, whose breakdown hover previews and a click
     * pins.
     */
    private Table chemistryRow(MissionComponent component, List<SetSpecialist> team) {
        Chemistry.Result chemistry = People.research(host.session.state(), component, team, null);
        Table row = new Table();
        row.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f), Theme.NAVY_BORDER,
            Theme.BORDER_THIN));
        row.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
        row.add(ui.label("TEAM CHEMISTRY", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(130f).left().top()
            .padTop(3f);
        row.add(ChemistryIndicator.withBreakdown(ui, chemistry, People.reasons(host.session.state(), chemistry)))
            .expandX().left();
        return row;
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
        // Released only once the team has gone; past the known cap with people seated, a hidden
        // Perfectionist keeps them working.
        if (card.atCap() && team(card).isEmpty()) {
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
        dialogCard.add(ui.paragraph("The engineer takes slot " + (card.nextFreeSlot() + 1) + ". Sorted by "
            + SetSkills.label(component.associatedSkill()) + ", the skill this component needs.",
            Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        List<SetSpecialist> team = team(card);
        Chemistry.Result now = People.research(host.session.state(), component, team, null);
        if (!team.isEmpty()) {
            Table teamNow = new Table();
            teamNow.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f), Theme.NAVY_BORDER,
                Theme.BORDER_THIN));
            teamNow.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
            teamNow.add(ui.label("THE TEAM NOW", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(Theme.SPACE_4);
            teamNow.add(ui.label(String.join(", ", team.stream().map(engineer -> People.surname(engineer.getName()))
                .toList()), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            teamNow.add(ChemistryIndicator.full(ui, now));
            dialogCard.add(teamNow).growX().padBottom(Theme.SPACE_3).row();
        }
        Table list = new Table();
        list.defaults().left();
        list.add();
        list.add(header("Idle engineer · traits")).left();
        list.add(header("With this team")).left();
        list.add(header("Skill")).left();
        list.add(header(team.isEmpty() ? "Chemistry" : "Chemistry with them")).left();
        list.add(header("Seasons to cap")).left();
        list.add().row();
        list.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(ui.solid(Theme.NAVY))).colspan(7).growX()
            .height(Theme.BORDER_THIN).padTop(4f).row();
        List<String> teamIds = team.stream().map(Personnel::getId).toList();
        java.util.OptionalInt nowToCap = ResearchPlanning.project(host.session.state(), component,
            card.owned().getCurrentReliability(), seats(card, null)).seasonsTo(cap(component, card, null));
        for (SetSpecialist specialist : candidates.subList(0, Math.min(6, candidates.size()))) {
            list.add(RosterCells.avatar(ui, specialist)).size(36f).padRight(Theme.SPACE_3).padTop(Theme.SPACE_2)
                .padBottom(Theme.SPACE_2);
            Table who = new Table();
            who.left();
            who.defaults().left();
            who.add(ui.label(specialist.getName(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            List<Trait> traits = People.traits(specialist);
            if (!traits.isEmpty()) {
                who.add(TraitChip.row(ui, traits, 260f)).padTop(4f).row();
            }
            if (People.gettingToKnow(specialist)) {
                who.add(ui.label("Still getting to know each other", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED))
                    .padTop(2f);
            }
            list.add(who).width(270f).padRight(Theme.SPACE_3);
            Table marks = new Table();
            marks.left();
            List<People.Relationship> relationships = People.marks(host.session.state(), specialist, teamIds);
            for (People.Relationship relationship : relationships) {
                marks.add(RelationshipMarker.mark(ui, relationship.positive(), relationship.partner().getName(),
                    "Already on this team.")).left().padRight(Theme.SPACE_3);
            }
            if (relationships.isEmpty()) {
                marks.add(ui.label("—", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            }
            list.add(marks).width(170f).padRight(Theme.SPACE_3);
            Table skill = new Table();
            String icon = SetSkills.icon(component.associatedSkill());
            com.badlogic.gdx.scenes.scene2d.ui.Image glyph = new com.badlogic.gdx.scenes.scene2d.ui.Image(
                ui.icon(icon), Scaling.fit);
            glyph.setColor(Theme.NAVY);
            skill.add(glyph).height(14f).width(14f * Math.min(1f, ui.iconAspect(icon))).padRight(6f);
            skill.add(ui.label(String.valueOf(Math.round(SetDepartment.skill(specialist, component.associatedSkill()))),
                Theme.TextStyle.BODY_SM, Theme.INK));
            list.add(skill).width(70f).padRight(Theme.SPACE_3);
            Chemistry.Result then = People.research(host.session.state(), component, team, specialist);
            list.add(team.isEmpty() ? ChemistryIndicator.compact(ui, then) : ChemistryIndicator.preview(ui, now, then))
                .width(210f).padRight(Theme.SPACE_3);
            list.add(toCap(card, specialist, nowToCap)).width(120f).padRight(Theme.SPACE_3);
            list.add(button("Assign", Theme.ButtonKind.PRIMARY, () -> host.submit(
                new AssignResearchAction(specialist.getId(), component.id()), result -> {
                    host.closeDialog();
                    host.toast("Assigned " + specialist.getName() + " to " + ContentNames.of(component.id()) + ".",
                        Theme.NoticeKind.SUCCESS);
                }))).right().row();
            list.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(ui.solid(Theme.NAVY_BORDER))).colspan(7).growX()
                .height(Theme.BORDER_THIN).row();
        }
        dialogCard.add(list).growX().padBottom(Theme.SPACE_3).row();
        dialogCard.add(ui.paragraph("Chemistry counts only what has come out: revealed traits and relationships that "
            + "show. \u201cMay differ\u201d means someone new to the team is still an unknown.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX().padBottom(Theme.SPACE_3).row();
        dialogCard.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).right().fill(false, false);
        host.showDialog(dialogCard, ASSIGN_DIALOG_WIDTH);
    }

    private Label header(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
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
        host.game.gateway().submit(host.session.credentials(), queue.poll(), host.session.openTurn(), result -> {
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
