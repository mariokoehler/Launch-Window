package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.actions.ReassignControlSlotAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.SkillBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.MissionControl;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.MissionAssessment;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionStaffing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Re-staff mission control ({@code ui-spec.md} &sect;4.7.3, decided 2026-10-03): a scheduled or
 * in-flight mission's seats and consoles as in Assemble mission, with the crew locked and every
 * console still in use reassignable to a free person of its type, at no cost. Each pick is one
 * {@link ReassignControlSlotAction}, sent at once; the person replaced goes idle. Released and
 * not-required consoles can't be touched. The rule only accepts idle people and can't leave a
 * console empty, so the picker lists free people and has no "Empty this slot".
 *
 * @author Mario Koehler
 */
public final class RestaffScreen extends ChromeScreen {

    private static final int COLUMNS = 4;

    private String missionId;
    /** The consoles changed during this visit, as slot titles with who took over (kept on screen only). */
    private final List<String> changes = new ArrayList<>();
    private final List<Integer> changedSlots = new ArrayList<>();

    /**
     * Builds the screen from the session.
     *
     * @param game      the game, in a session
     * @param missionId the mission to re-staff
     */
    public RestaffScreen(LaunchWindowGame game, String missionId) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.MISSION_CONTROL, new Crumb("Re-staff", null)),
            "Mission Control", "Re-staff mission control");
        this.missionId = missionId;
        rebuild();
    }

    @Override
    protected void goBack() {
        InProgressMission mission = session.state().getInProgressMissions().get(missionId);
        game.setScreen(new MissionsScreen(game, mission == null || mission.getCurrentSeasonIndex() == 0));
    }

    @Override
    protected void rebuild() {
        PlayerSnapshot state = session.state();
        content.clearChildren();
        content.top();
        content.defaults().growX();
        InProgressMission mission = state.getInProgressMissions().get(missionId);
        if (mission == null) {
            Table gone = ui.card();
            gone.pad(Theme.SPACE_6);
            gone.add(ui.paragraph("That mission isn't scheduled or in flight any more.", Theme.TextStyle.BODY,
                Theme.INK_MUTED));
            content.add(gone);
            return;
        }
        MissionConfiguration configuration = session.catalog().missionConfiguration(mission.getMissionConfigurationId())
            .orElseThrow();

        Table toolbar = new Table();
        List<InProgressMission> all = new ArrayList<>(MissionControl.scheduled(state));
        all.addAll(MissionControl.active(state));
        if (all.size() > 1) {
            for (InProgressMission other : all) {
                MissionConfiguration otherConfiguration = session.catalog().missionConfiguration(
                    other.getMissionConfigurationId()).orElseThrow();
                boolean active = other.getId().equals(missionId);
                Table chip = new Table();
                chip.setBackground(active ? ui.solid(Theme.SURFACE_CARD)
                    : ui.rect(new Color(0f, 0f, 0f, 0f), Theme.ON_PAGE, Theme.BORDER_THIN));
                chip.pad(5f, 12f, 5f, 12f);
                chip.add(ui.label(MissionControl.missionName(otherConfiguration) + (other.getCurrentSeasonIndex() == 0
                    ? " · scheduled" : " · in flight"), Theme.TextStyle.LABEL, active ? Theme.NAVY
                    : Theme.ON_PAGE_STRONG));
                chip.setTouchable(Touchable.enabled);
                chip.addListener(clicked(() -> {
                    missionId = other.getId();
                    changes.clear();
                    changedSlots.clear();
                    rebuild();
                }));
                toolbar.add(chip).padRight(6f);
            }
        }
        toolbar.add(ui.paragraph("The crew stays as launched. Any console still in use can go to a free person of its "
            + "type, at no cost; the person replaced goes idle.", Theme.TextStyle.BODY_SM, Theme.ON_PAGE_STRONG))
            .expandX().fillX().right().padLeft(Theme.SPACE_4);
        content.add(toolbar).padBottom(Theme.SPACE_3).row();

        Table columns = new Table();
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        left.add(summary(mission, configuration)).row();
        left.add(crew(mission, configuration)).row();
        if (!changes.isEmpty()) {
            Table changed = ui.card();
            changed.defaults().growX().left();
            changed.add(ui.label("CHANGED TODAY", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2)
                .row();
            for (String change : changes) {
                changed.add(ui.paragraph(change, Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(2f).row();
            }
            left.add(changed).row();
        }
        columns.add(scrollPane(left)).width(520f).top().fillY().padRight(Theme.SPACE_5);
        columns.add(scrollPane(consoles(mission, configuration))).expand().fill();
        content.add(columns).expand().fill();
    }

    private Table summary(InProgressMission mission, MissionConfiguration configuration) {
        PlayerSnapshot state = session.state();
        Table card = ui.card();
        card.defaults().growX().left();
        boolean scheduled = mission.getCurrentSeasonIndex() == 0;
        card.add(ui.label((scheduled ? "Scheduled · flies at the next turn" : "In flight · season "
            + (mission.getCurrentSeasonIndex() + 1) + " of " + configuration.seasons().size() + " next")
            .toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        card.add(ui.label(MissionControl.missionName(configuration), Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(Theme.SPACE_3).row();
        MissionAssessment.Assessment odds = MissionAssessment.assess(state, session.catalog(), mission, configuration);
        Table stats = new Table();
        stats.left();
        Table complete = new Table();
        complete.defaults().left();
        complete.add(ui.label(MissionPlanScreen.pct(odds.completion()), Theme.TextStyle.HEADING_LG, Theme.INK)).row();
        complete.add(ui.label("COMPLETES", Theme.TextStyle.LABEL, Theme.INK_MUTED));
        stats.add(complete).padRight(22f);
        if (!mission.crewIds().isEmpty()) {
            Table loss = new Table();
            loss.defaults().left();
            loss.add(ui.label(MissionPlanScreen.pct(odds.crewLoss()), Theme.TextStyle.HEADING_LG, Theme.GOLD_TEXT))
                .row();
            loss.add(ui.label("CREW LOST", Theme.TextStyle.LABEL, Theme.INK_MUTED));
            stats.add(loss);
        }
        card.add(stats).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("Odds for the seasons still to fly, with the people on the consoles now. A change counts "
            + "at once.", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return card;
    }

    private Table crew(InProgressMission mission, MissionConfiguration configuration) {
        PlayerSnapshot state = session.state();
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label("FLIGHT CREW · LOCKED", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3)
            .row();
        if (configuration.crewSlots().isEmpty()) {
            card.add(ui.paragraph("Uncrewed: nobody flies aboard.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        }
        for (int seat = 0; seat < configuration.crewSlots().size(); seat++) {
            String id = seat < mission.getCrewSlotIds().size() ? mission.getCrewSlotIds().get(seat) : null;
            Astronaut astronaut = find(state.getAstronauts(), id);
            card.add(SlotTile.build(ui, configuration.crewSlots().get(seat), "Astronaut", true, null, "Seat fit",
                astronaut == null ? null : astronaut.getName(), astronaut == null ? null
                    : String.valueOf(Math.round(MissionStaffing.seatFit(astronaut, configuration, seat))),
                astronaut == null ? SlotTile.State.EMPTY : SlotTile.State.FILLED)).padBottom(4f).row();
        }
        return card;
    }

    private Table consoles(InProgressMission mission, MissionConfiguration configuration) {
        PlayerSnapshot state = session.state();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(ui.label("Mission control", Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_3).row();
        Table grid = new Table();
        grid.top().left();
        for (int slot = 0; slot < configuration.controlSlots().size(); slot++) {
            StaffingSlot staffing = configuration.controlSlots().get(slot);
            String id = slot < mission.getControlSlotIds().size() ? mission.getControlSlotIds().get(slot) : null;
            Personnel person = staffing.kind() == StaffingSlot.Kind.ASTRONAUT ? find(state.getAstronauts(), id)
                : find(state.getMissionControlOfficers(), id);
            boolean released = MissionControl.released(mission, configuration, slot);
            boolean used = configuration.usedControlSlots().contains(slot);
            SlotTile.State tileState;
            if (!used) {
                tileState = SlotTile.State.UNUSED;
            } else if (released) {
                tileState = SlotTile.State.RELEASED;
            } else if (person == null) {
                tileState = free(staffing.kind()).isEmpty() ? SlotTile.State.NOBODY : SlotTile.State.EMPTY;
            } else {
                tileState = changedSlots.contains(slot) ? SlotTile.State.CHANGED : SlotTile.State.FILLED;
            }
            String value = person == null ? null : String.valueOf(Math.round(MissionProgress.slotValue(state, staffing,
                id)));
            Table tile = SlotTile.build(ui, staffing.title(), staffing.kind() == StaffingSlot.Kind.ASTRONAUT
                    ? "Astronaut" : "Flight controller", staffing.kind() == StaffingSlot.Kind.ASTRONAUT,
                CrewSkills.icon(staffing.skill()), CrewSkills.label(staffing.skill()),
                person == null ? null : person.getName(), value, tileState);
            if (used && !released) {
                int index = slot;
                tile.setTouchable(Touchable.enabled);
                tile.addListener(clicked(() -> openPicker(index)));
            }
            grid.add(tile).expandX().fillX().uniformX().top().pad(0f, 0f, 10f, slot % COLUMNS < COLUMNS - 1 ? 10f : 0f);
            if (slot % COLUMNS == COLUMNS - 1) {
                grid.row();
            }
        }
        for (int slot = configuration.controlSlots().size(); slot % COLUMNS != 0; slot++) {
            grid.add().expandX().fillX().uniformX();
        }
        card.add(grid).row();
        card.add(ui.paragraph("Released consoles were last needed in an earlier season; their holders went idle then, "
            + "and they stay empty. Not-required consoles are never used by this mission.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).padTop(Theme.SPACE_2);
        return card;
    }

    private List<? extends Personnel> free(StaffingSlot.Kind kind) {
        PlayerSnapshot state = session.state();
        List<? extends Personnel> roster = kind == StaffingSlot.Kind.ASTRONAUT ? state.getAstronauts()
            : state.getMissionControlOfficers();
        return roster.stream().filter(MissionLaunch::isAvailable).toList();
    }

    /**
     * Opens the picker for a console: the free people of its type, best at its skill first. A pick
     * is sent at once.
     *
     * @param slot the console
     */
    public void openPicker(int slot) {
        PlayerSnapshot state = session.state();
        InProgressMission mission = state.getInProgressMissions().get(missionId);
        if (mission == null) {
            return;
        }
        MissionConfiguration configuration = session.catalog().missionConfiguration(mission.getMissionConfigurationId())
            .orElseThrow();
        StaffingSlot staffing = configuration.controlSlots().get(slot);
        boolean astronaut = staffing.kind() == StaffingSlot.Kind.ASTRONAUT;
        String skill = CrewSkills.label(staffing.skill());
        String previousId = slot < mission.getControlSlotIds().size() ? mission.getControlSlotIds().get(slot) : null;
        String previous = name(previousId);
        Table card = dialogCard(staffing.title(), astronaut ? "Pick an astronaut" : "Pick a flight controller");
        card.add(ui.paragraph("Free people only. Sorted by " + skill + ", the value the roll will read."
            + (previous == null ? "" : " " + previous + " goes idle."), Theme.TextStyle.BODY, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_3).row();
        List<? extends Personnel> people = free(staffing.kind()).stream()
            .sorted(Comparator.comparingDouble((Personnel person) -> MissionProgress.slotValue(state, staffing,
                person.getId())).reversed()).toList();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        for (Personnel person : people) {
            double value = MissionProgress.slotValue(state, staffing, person.getId());
            Table row = new Table();
            row.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            row.pad(8f, 10f, 8f, 10f);
            row.add(ui.label(person.getName(), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
            row.add(ui.label(skill, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(Theme.SPACE_2);
            row.add(new SkillBar(ui, value)).width(120f).height(8f).padRight(Theme.SPACE_2);
            row.add(ui.label(String.valueOf(Math.round(value)), Theme.TextStyle.BODY_STRONG, Theme.NAVY)).width(28f);
            row.setTouchable(Touchable.enabled);
            row.addListener(clicked(() -> submit(new ReassignControlSlotAction(missionId, slot, person.getId()),
                result -> {
                    closeDialog();
                    changedSlots.add(slot);
                    changes.add(staffing.title() + ": " + person.getName() + (previous == null ? "" : ", replacing "
                        + previous));
                    rebuild();
                    toast(person.getName() + " is on " + staffing.title() + "." + (previous == null ? ""
                        : " " + previous + " is idle."), Theme.NoticeKind.SUCCESS);
                })));
            list.add(row).padBottom(4f).row();
        }
        if (people.isEmpty()) {
            list.add(ui.notice(Theme.NoticeKind.WARNING, "Nobody is free.", astronaut
                ? "Every astronaut is busy. Hire more in the Astronaut Complex."
                : "Every flight controller is busy. Hire more in Mission Control."));
        }
        card.add(scrollPane(list)).maxHeight(420f).padBottom(Theme.SPACE_3).row();
        card.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).right().fill(false, false);
        showDialog(card);
    }

    private String name(String id) {
        if (id == null) {
            return null;
        }
        PlayerSnapshot state = session.state();
        return Stream.concat(state.getAstronauts().stream(), state.getMissionControlOfficers().stream())
            .filter(person -> person.getId().equals(id)).map(Personnel::getName).findFirst().orElse(null);
    }

    private static <P extends Personnel> P find(List<P> roster, String id) {
        if (id == null) {
            return null;
        }
        for (P person : roster) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }
}
