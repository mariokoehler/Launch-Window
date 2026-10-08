package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.CancelMissionAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.MissionAssessment;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionCancellation;
import de.mkoehler.launchwindow.rules.MissionLaunch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Mission Control's scheduled missions ({@code ui-spec.md} &sect;4.7.2) or active missions
 * (&sect;4.7.3): a card per mission with its launches, people, odds and what it cost, and the way
 * to re-staff it. A scheduled mission can be scrubbed for a full refund; an uncrewed one in flight
 * can be cancelled for its refund share and a prestige loss; a crewed one in flight can't be
 * abandoned. What each costs or returns is {@link MissionCancellation#preview}'s, the server's own.
 *
 * @author Mario Koehler
 */
public final class MissionsScreen extends ChromeScreen {

    private final boolean scheduled;

    /**
     * Builds the screen from the session.
     *
     * @param game      the game, in a session
     * @param scheduled whether to show the scheduled missions (else the active ones)
     */
    public MissionsScreen(LaunchWindowGame game, boolean scheduled) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.MISSION_CONTROL,
            new Crumb(scheduled ? "Scheduled missions" : "Active missions", null)), "Mission Control",
            scheduled ? "Scheduled missions" : "Active missions");
        this.scheduled = scheduled;
        rebuild();
    }

    @Override
    protected void goBack() {
        goMissionControl();
    }

    private List<InProgressMission> missions() {
        return scheduled ? MissionControl.scheduled(session.state()) : MissionControl.active(session.state());
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.MISSION_CONTROL;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        List<InProgressMission> missions = missions();
        Table list = new Table();
        list.top();
        list.defaults().growX().padBottom(Theme.SPACE_4);
        if (missions.isEmpty()) {
            list.add(empty()).row();
        }
        for (InProgressMission mission : missions) {
            list.add(card(mission)).row();
        }
        if (scheduled && !missions.isEmpty()) {
            list.add(ui.paragraph("You can scrub or re-staff until the turn resolves. After that, a mission is in "
                + "flight and moves to Active missions.", Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG)).row();
        }
        content.add(scrollPane(list)).expand().fill();
    }

    private Table empty() {
        Table card = ui.card();
        card.pad(Theme.SPACE_6);
        card.add(ui.label(scheduled ? "No missions scheduled." : "No missions in flight.", Theme.TextStyle.HEADING_SM,
            Theme.INK)).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(scheduled ? "Missions you launch in Headquarters wait here until the next turn, when they "
            + "fly." : "A mission moves here after its first season flies, and stays until it ends.",
            Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        if (scheduled) {
            card.add(actionButton("Plan a mission in Headquarters", Theme.ButtonKind.SECONDARY, this::goHeadquarters))
                .fill(false, false);
        }
        return card;
    }

    private Table card(InProgressMission mission) {
        PlayerSnapshot state = session.state();
        MissionConfiguration configuration = session.catalog().missionConfiguration(mission.getMissionConfigurationId())
            .orElseThrow();
        boolean crewed = !mission.crewIds().isEmpty();
        Table card = ui.card();
        card.top();
        Drawable art = ui.croppedPicture("configurations/" + configuration.id() + ".jpg", 300f / 200f);
        card.add(new Image(art == null ? ui.solid(Theme.NAVY) : art)).size(300f, 200f).top().padRight(Theme.SPACE_5);

        Table middle = new Table();
        middle.top().left();
        middle.defaults().left().growX();
        Table head = new Table();
        Table titles = new Table();
        titles.left();
        titles.defaults().left();
        String eyebrow = scheduled ? "Launched · flies at the next turn"
            : "In flight · " + (crewed ? "crewed" : "uncrewed");
        titles.add(ui.label(eyebrow.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        titles.add(ui.label(MissionControl.missionName(configuration), Theme.TextStyle.HEADING_LG, Theme.INK));
        head.add(titles).expandX().left();
        head.add(ui.badge(Theme.BadgeKind.ACTIVE, scheduled ? "Scheduled" : "In flight")).top();
        middle.add(head).padBottom(Theme.SPACE_3).row();
        if (!scheduled) {
            middle.add(track(mission, configuration)).left().padBottom(Theme.SPACE_3).row();
        }
        Table kv = new Table();
        kv.left();
        for (int i = 0; i < configuration.launches().size(); i++) {
            MissionConfiguration.Launch launch = configuration.launches().get(i);
            String rocket = i >= mission.getRocketIds().size() ? "?" : mission.getRocketIds().get(i).isEmpty()
                ? "Carrier aircraft" : ContentNames.of(mission.getRocketIds().get(i));
            kv(kv, "Launch " + (i + 1), rocket + " to " + Missions.launchLabel(launch.launchType()).toLowerCase(Locale.ROOT)
                + " · payload " + String.join(", ", launch.payloadComponentIds().stream().map(ContentNames::of).toList()));
        }
        if (!scheduled) {
            int season = mission.getCurrentSeasonIndex();
            int steps = configuration.seasons().get(Math.min(season, configuration.seasons().size() - 1)).steps().size();
            kv(kv, "Next turn", "Season " + (season + 1) + " of " + configuration.seasons().size() + ": " + steps
                + (steps == 1 ? " step." : " steps."));
        }
        kv(kv, "Prestige", String.format(Locale.ROOT, "%,d", Missions.nextPrestige(configuration, state))
            + " if it completes" + (Missions.flown(configuration, state) == 0 ? " (first flight)" : ""));
        middle.add(kv).padBottom(Theme.SPACE_3).row();
        if (!scheduled) {
            middle.add(components(mission, configuration)).padBottom(Theme.SPACE_3).row();
        }
        middle.add(ui.label(crewed || !configuration.crewSlots().isEmpty() ? "CREW AND MISSION CONTROL"
            : "MISSION CONTROL", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        middle.add(people(mission, configuration));
        card.add(middle).expandX().fillX().top().padRight(Theme.SPACE_5);
        card.add(side(mission, configuration, crewed)).width(360f).top();
        return card;
    }

    private void kv(Table kv, String key, String value) {
        kv.add(ui.label(key.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(96f).top().left()
            .padTop(3f).padBottom(4f).padRight(10f);
        kv.add(ui.paragraph(value, Theme.TextStyle.BODY_SM, Theme.INK)).growX().padBottom(4f).row();
    }

    private Table track(InProgressMission mission, MissionConfiguration configuration) {
        Table track = new Table();
        track.left();
        int current = mission.getCurrentSeasonIndex();
        for (int season = 0; season < configuration.seasons().size(); season++) {
            if (season > 0) {
                track.add(new Image(ui.progressDash(season == current))).width(40f).height(2f).padLeft(4f).padRight(4f);
            }
            Theme.ProgressState state = season < current ? Theme.ProgressState.DONE
                : season == current ? Theme.ProgressState.CURRENT : Theme.ProgressState.PENDING;
            Table step = new Table();
            step.add(new Image(ui.progressDot(state))).size(14f).row();
            step.add(ui.label("S" + (season + 1), Theme.TextStyle.LABEL, Theme.INK_MUTED)).padTop(2f);
            track.add(step);
        }
        return track;
    }

    private Table components(InProgressMission mission, MissionConfiguration configuration) {
        Table box = new Table();
        box.left();
        box.defaults().left().growX();
        Set<String> later = new LinkedHashSet<>();
        Set<String> all = new LinkedHashSet<>();
        for (int season = 0; season < configuration.seasons().size(); season++) {
            for (MissionStep step : configuration.seasons().get(season).steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String id = MissionLaunch.resolveComponentId(involvement.componentId(), mission.getRocketIds());
                    all.add(id);
                    if (season >= mission.getCurrentSeasonIndex()) {
                        later.add(id);
                    }
                }
            }
        }
        for (String id : all) {
            MissionComponent component = session.catalog().missionComponent(id).orElse(null);
            OwnedMissionComponent owned = session.state().getMissionComponents().get(id);
            if (component == null || owned == null) {
                continue;
            }
            Table head = new Table();
            Image glyph = new Image(ui.icon(SetSkills.icon(component.associatedSkill())), Scaling.fit);
            glyph.setColor(Theme.NAVY);
            head.add(glyph).size(14f).padRight(6f);
            head.add(ui.label(ContentNames.of(id), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
            // Still to fly: the copy built at launch, which ground R&D and other flights don't change.
            double shown = later.contains(id) ? MissionProgress.flyingReliability(session.state(), mission, id)
                : owned.getCurrentReliability();
            head.add(ui.label(String.format(Locale.ROOT, "%.1f%%", shown), Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            box.add(head).padBottom(2f).row();
            if (later.contains(id)) {
                box.add(new ReliabilityBar(ui, shown, component.maxRdReliability(),
                    component.maxReliability(), owned.getFloorReliability())).height(12f).padBottom(2f).row();
                String note = Math.abs(owned.getCurrentReliability() - shown) >= 0.05
                    ? String.format(Locale.ROOT, "Flies as built at launch. On the ground it's %.1f%% now, for later "
                        + "missions.", owned.getCurrentReliability())
                    : "Flies as built at launch.";
                box.add(ui.label(note, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(6f).row();
            } else {
                box.add(ui.label("Done flying on this mission; its reliability change was applied then.",
                    Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(6f).row();
            }
        }
        return box;
    }

    private Table people(InProgressMission mission, MissionConfiguration configuration) {
        PlayerSnapshot state = session.state();
        Table grid = new Table();
        grid.left();
        int columns = 4;
        int index = 0;
        for (int seat = 0; seat < configuration.crewSlots().size(); seat++) {
            String id = seat < mission.getCrewSlotIds().size() ? mission.getCrewSlotIds().get(seat) : null;
            grid.add(person(configuration.crewSlots().get(seat), "astronauts", name(state, id), true))
                .expandX().fillX().uniformX().pad(0f, 0f, 6f, 6f);
            if (++index % columns == 0) {
                grid.row();
            }
        }
        for (int slot = 0; slot < configuration.controlSlots().size(); slot++) {
            StaffingSlot staffing = configuration.controlSlots().get(slot);
            String id = slot < mission.getControlSlotIds().size() ? mission.getControlSlotIds().get(slot) : null;
            String holder;
            if (!configuration.usedControlSlots().contains(slot)) {
                holder = "Not required";
            } else if (MissionControl.released(mission, configuration, slot)) {
                // The season pass empties a released console, so there's no name left to show.
                holder = "Released";
            } else {
                holder = Objects.requireNonNullElse(name(state, id), "Empty");
            }
            grid.add(person(staffing.title(), CrewSkills.icon(staffing.skill()), holder,
                staffing.kind() == StaffingSlot.Kind.ASTRONAUT)).expandX().fillX().uniformX().pad(0f, 0f, 6f, 6f);
            if (++index % columns == 0) {
                grid.row();
            }
        }
        return grid;
    }

    private Table person(String title, String icon, String holder, boolean astronaut) {
        Table cell = new Table();
        cell.setBackground(ui.rect(Theme.SURFACE_CARD, astronaut ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        cell.pad(6f, 8f, 6f, 8f);
        cell.left();
        cell.defaults().left();
        Table head = new Table();
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(astronaut ? Theme.NAVY : Theme.INK_MUTED);
        head.add(glyph).size(11f).padRight(4f);
        head.add(ui.label(title, Theme.TextStyle.LABEL, astronaut ? Theme.NAVY : Theme.INK));
        cell.add(head).padBottom(2f).row();
        cell.add(ui.label(holder == null ? "Empty" : holder, Theme.TextStyle.BODY_SM, holder == null
            || holder.startsWith("Not required") || holder.startsWith("Released") ? Theme.INK_MUTED : Theme.INK));
        return cell;
    }

    private static String name(PlayerSnapshot state, String id) {
        if (id == null) {
            return null;
        }
        return Stream.concat(state.getAstronauts().stream(), state.getMissionControlOfficers().stream())
            .filter(person -> person.getId().equals(id)).map(Personnel::getName).findFirst().orElse(null);
    }

    /**
     * Sums a mission's people up in a line ("Pavel Orlov aboard · CAPCOM Grigori Zaitsev · 9
     * controllers").
     *
     * @param mission       the mission
     * @param configuration its configuration
     * @param state         the player's state
     * @return the line
     */
    static String peopleLine(InProgressMission mission, MissionConfiguration configuration, PlayerSnapshot state) {
        List<String> parts = new ArrayList<>();
        List<String> crew = mission.crewIds().stream().map(id -> name(state, id)).filter(Objects::nonNull).toList();
        if (!crew.isEmpty()) {
            parts.add(String.join(", ", crew) + " aboard");
        } else {
            parts.add("Uncrewed");
        }
        int controllers = 0;
        for (int slot = 0; slot < configuration.controlSlots().size() && slot < mission.getControlSlotIds().size();
             slot++) {
            String id = mission.getControlSlotIds().get(slot);
            if (id == null || MissionControl.released(mission, configuration, slot)) {
                continue;
            }
            if (configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT) {
                parts.add(configuration.controlSlots().get(slot).title() + " " + name(state, id));
            } else {
                controllers++;
            }
        }
        parts.add(controllers + (controllers == 1 ? " controller" : " controllers"));
        return String.join(" · ", parts);
    }

    private Table side(InProgressMission mission, MissionConfiguration configuration, boolean crewed) {
        PlayerSnapshot state = session.state();
        Table side = new Table();
        side.top();
        side.defaults().growX().left();
        MissionAssessment.Assessment odds = MissionAssessment.assess(state, session.catalog(), mission, configuration);
        side.add(ui.label((scheduled ? "ODDS WITH THIS STAFF" : "ODDS FOR WHAT'S LEFT"), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table stats = new Table();
        stats.left();
        stats.add(stat(MissionPlanScreen.pct(odds.completion()), "Completes", Theme.INK)).padRight(22f);
        if (crewed) {
            stats.add(stat(MissionPlanScreen.pct(odds.crewLoss()), "Crew lost", Theme.GOLD_TEXT));
        }
        side.add(stats).padBottom(Theme.SPACE_2).row();
        for (MissionConfiguration.GoalModifier dependency : configuration.goalDependencies()) {
            if (!state.hasAchievedOrCancelledGoal(dependency.goalId())) {
                side.add(ui.paragraph(ContentNames.of(dependency.goalId()) + " isn't reached yet, so "
                    + ContentNames.of(dependency.componentId()) + " flies " + dependency.amount() + " points lower.",
                    Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
            }
        }
        MissionCancellation.Preview preview = MissionCancellation.preview(state, mission.getId(), session.catalog());
        if (scheduled) {
            side.add(ui.label("PAID AT LAUNCH", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(Theme.SPACE_3)
                .padBottom(Theme.SPACE_2).row();
            List<Programs.PriceLine> lines = new ArrayList<>();
            lines.add(new Programs.PriceLine(Missions.name(configuration) + " configuration", configuration.cost(),
                null));
            for (String rocketId : mission.getRocketIds()) {
                session.catalog().missionComponent(rocketId).ifPresent(rocket -> lines.add(new Programs.PriceLine(
                    ContentNames.of(rocketId) + ", unit cost", rocket.unitCost(), null)));
            }
            Table breakdown = priceBreakdown(lines, preview.refund());
            side.add(breakdown).padBottom(2f).row();
            side.add(ui.label("Scrubbing refunds it all.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padBottom(Theme.SPACE_4).row();
        }
        Table buttons = new Table();
        buttons.left();
        if (!scheduled) {
            buttons.add(actionButton("Replay season " + mission.getCurrentSeasonIndex(), Theme.ButtonKind.SECONDARY,
                () -> openLatestReport(mission.getId()))).padRight(Theme.SPACE_2).padBottom(Theme.SPACE_2);
        }
        buttons.add(actionButton("Re-staff mission control", Theme.ButtonKind.SECONDARY,
            () -> game.setScreen(new RestaffScreen(game, mission.getId())))).padRight(Theme.SPACE_2)
            .padBottom(Theme.SPACE_2);
        if (preview.outcome() == MissionCancellation.Outcome.SCRUBBED) {
            buttons.row();
            buttons.add(actionButton("Scrub", Theme.ButtonKind.DESTRUCTIVE, () -> confirmCallOff(mission))).left();
        } else if (preview.outcome() == MissionCancellation.Outcome.CANCELLED) {
            buttons.row();
            buttons.add(actionButton("Cancel mission", Theme.ButtonKind.DESTRUCTIVE, () -> confirmCallOff(mission)))
                .left();
        }
        side.add(buttons).padTop(Theme.SPACE_2).row();
        if (preview.outcome() == MissionCancellation.Outcome.CREWED_MISSION_IN_FLIGHT) {
            side.add(ui.label("Can't be abandoned", Theme.TextStyle.BODY_STRONG, Theme.INK)).padTop(Theme.SPACE_3)
                .row();
            side.add(ui.paragraph("A crewed mission flies until it ends: there is no Cancel. Re-staffing mission "
                + "control is still possible.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        }
        return side;
    }

    private Table stat(String value, String label, com.badlogic.gdx.graphics.Color color) {
        Table stat = new Table();
        stat.defaults().left();
        stat.add(ui.label(value, Theme.TextStyle.HEADING_LG, color)).row();
        stat.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED));
        return stat;
    }

    /**
     * Asks to scrub a scheduled mission, or cancel an uncrewed one in flight, with what it returns
     * and costs.
     *
     * @param mission the mission
     */
    public void confirmCallOff(InProgressMission mission) {
        PlayerSnapshot state = session.state();
        MissionConfiguration configuration = session.catalog().missionConfiguration(mission.getMissionConfigurationId())
            .orElseThrow();
        MissionCancellation.Preview preview = MissionCancellation.preview(state, mission.getId(), session.catalog());
        boolean scrub = preview.outcome() == MissionCancellation.Outcome.SCRUBBED;
        String name = MissionControl.missionName(configuration);
        List<String> people = new ArrayList<>();
        mission.crewIds().forEach(id -> people.add(name(state, id)));
        List<String> controllers = new ArrayList<>();
        for (int slot = 0; slot < mission.getControlSlotIds().size(); slot++) {
            String id = mission.getControlSlotIds().get(slot);
            if (id == null || slot >= configuration.controlSlots().size()
                || MissionControl.released(mission, configuration, slot)) {
                continue;
            }
            if (configuration.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT) {
                people.add(name(state, id));
            } else {
                controllers.add(name(state, id));
            }
        }
        people.removeIf(Objects::isNull);
        controllers.removeIf(Objects::isNull);
        int headcount = people.size() + controllers.size();
        // One or two controllers are named; more are counted.
        if (controllers.size() <= 2) {
            people.addAll(controllers);
        } else {
            people.add("the " + controllers.size() + " controllers");
        }
        String who = people.size() <= 1 ? String.join("", people)
            : String.join(", ", people.subList(0, people.size() - 1)) + " and " + people.get(people.size() - 1);
        Table card = dialogCard("Confirm", (scrub ? "Scrub " : "Cancel ") + name + "?");
        String body = scrub
            ? "It won't fly at the next turn. You get the full " + money(preview.refund()) + " back"
            : "It ends now, so the seasons left don't fly and their prestige isn't earned";
        body += who.isEmpty() ? "." : (scrub ? ", and " : ". ") + capitalised(who, !scrub) + (headcount == 1
            ? " goes" : " go") + " back to idle.";
        card.add(ui.paragraph(body, Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        if (!scrub) {
            Table lines = new Table();
            lines.defaults().growX();
            lines.add(priceRow("Prestige lost", "− " + preview.prestigeLost())).padBottom(4f).row();
            lines.add(priceRow("Refund: " + configuration.cancellationRefundPercent() + " % of "
                + money(mission.getLaunchCost()) + " paid at launch", money(preview.refund())));
            card.add(lines).padBottom(Theme.SPACE_4).row();
        }
        Table buttons = new Table();
        buttons.add(actionButton(scrub ? "Keep it" : "Keep flying", Theme.ButtonKind.SECONDARY, this::closeDialog))
            .padRight(10f);
        buttons.add(actionButton(scrub ? "Scrub" : "Cancel mission", Theme.ButtonKind.DESTRUCTIVE, () -> submit(
            new CancelMissionAction(mission.getId()), result -> {
                closeDialog();
                toast((scrub ? "Scrubbed " : "Cancelled ") + name + " — " + money(preview.refund())
                    + " back.", Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }

    private static String capitalised(String text, boolean capital) {
        return capital && !text.isEmpty() ? Character.toUpperCase(text.charAt(0)) + text.substring(1) : text;
    }

    private Table priceRow(String label, String value) {
        Table row = new Table();
        row.add(ui.label(label, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
        row.add(ui.label(value, Theme.TextStyle.BODY_SM, Theme.INK));
        return row;
    }
}
