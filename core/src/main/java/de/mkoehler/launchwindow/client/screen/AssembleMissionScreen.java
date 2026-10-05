package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.LaunchMissionAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.SkillBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.MissionDraft;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.MissionAssessment;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionStaffing;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Assemble mission ({@code ui-spec.md} &sect;4.6.8): the configuration's crew seats and consoles,
 * one to one, filled by hand or by Auto-assign, the chosen rockets, the odds for exactly these
 * people, and Confirm &amp; pay. The launch check is {@link MissionLaunch#check}, the server's own,
 * so the button is only live when the server would accept the launch.
 *
 * @author Mario Koehler
 */
public final class AssembleMissionScreen extends ChromeScreen {

    private static final int COLUMNS = 4;

    private final MissionConfiguration configuration;
    private final Program program;
    /** What was sent, kept on show once the launch went through. */
    private MissionDraft launched;

    /**
     * Builds the screen from the session.
     *
     * @param game            the game, in a session
     * @param configurationId the configuration
     */
    public AssembleMissionScreen(LaunchWindowGame game, String configurationId) {
        this(game, game.session().catalog().missionConfiguration(configurationId).orElseThrow());
    }

    private AssembleMissionScreen(LaunchWindowGame game, MissionConfiguration configuration) {
        super(game, MissionConfigsScreen.crumbs(configuration.programId(), configuration, "Assemble"),
            "Headquarters · " + ContentNames.of(configuration.programId()) + " · " + Missions.name(configuration),
            "Assemble mission");
        this.configuration = configuration;
        this.program = session.catalog().program(configuration.programId()).orElseThrow();
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(new MissionPlanScreen(game, configuration.id(), MissionPlanScreen.Tab.ASSESSMENT));
    }

    private boolean editable() {
        return launched == null && session.state().getOpenedProgramIds().contains(program.id());
    }

    private MissionDraft draft() {
        return launched != null ? launched : session.draft(configuration);
    }

    @Override
    protected void rebuild() {
        MissionDraft draft = draft();
        content.clearChildren();
        content.top();
        content.defaults().growX();

        Table toolbar = new Table();
        toolbar.add(ui.paragraph("Pick who flies and who runs each console. Each slot shows the one skill its holder "
            + "adds to the rolls.", Theme.TextStyle.BODY_SM, Theme.ON_PAGE_STRONG)).expandX().fillX().left();
        TextButton clear = onClick(ui.pageButton("Clear all"), () -> {
            draft.clear();
            rebuild();
        });
        TextButton auto = onClick(ui.pageButton("Auto-assign"), () -> {
            draft.autoAssign(session.state());
            rebuild();
        });
        clear.setDisabled(!editable());
        auto.setDisabled(!editable());
        toolbar.add(clear).padRight(Theme.SPACE_2);
        toolbar.add(auto);
        content.add(toolbar).padBottom(Theme.SPACE_3).row();
        if (!session.state().getOpenedProgramIds().contains(program.id()) && launched == null) {
            content.add(MissionConfigsScreen.readOnlyNotice(this, program, "Assembly is read-only until the program "
                + "is open.")).padBottom(Theme.SPACE_3).row();
        }
        String shortage = shortage();
        if (shortage != null && launched == null) {
            content.add(ui.notice(Theme.NoticeKind.WARNING, "Short of staff.", shortage)).padBottom(Theme.SPACE_3)
                .row();
        }

        Table columns = new Table();
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        left.add(crewCard(draft)).row();
        left.add(rocketCard(draft)).row();
        left.add(payCard(draft)).row();
        columns.add(scrollPane(left)).width(560f).top().fillY().padRight(Theme.SPACE_5);
        columns.add(scrollPane(controlCard(draft))).expand().fill();
        content.add(columns).expand().fill();
    }

    /**
     * Words a staff shortage as the spec does ("Only 5 flight controllers are free; this mission
     * needs 9. Upgrade Mission Control and hire."), or {@code null} when there's none.
     */
    private String shortage() {
        PlayerSnapshot state = session.state();
        List<String> lines = new ArrayList<>();
        int astronautsNeeded = Missions.crewCount(configuration) + Missions.capcomCount(configuration);
        long astronautsFree = state.getAstronauts().stream().filter(MissionLaunch::isAvailable).count();
        if (astronautsFree < astronautsNeeded) {
            lines.add("Only " + astronautsFree + (astronautsFree == 1 ? " astronaut is" : " astronauts are")
                + " free; this mission needs " + astronautsNeeded + ". Upgrade the Astronaut Complex and hire.");
        }
        int controllersNeeded = Missions.controllerCount(configuration);
        long controllersFree = state.getMissionControlOfficers().stream().filter(MissionLaunch::isAvailable).count();
        if (controllersFree < controllersNeeded) {
            lines.add("Only " + controllersFree + (controllersFree == 1 ? " flight controller is" : " flight controllers"
                + " are") + " free; this mission needs " + controllersNeeded + ". Upgrade Mission Control and hire.");
        }
        return lines.isEmpty() ? null : String.join(" ", lines);
    }

    // ---- Crew, rockets, pay

    private Table crewCard(MissionDraft draft) {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label("FLIGHT CREW", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        if (configuration.crewSlots().isEmpty()) {
            card.add(ui.paragraph("Uncrewed: nobody flies aboard.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (int slot = 0; slot < configuration.crewSlots().size(); slot++) {
            boolean used = configuration.usedCrewSlots().contains(slot);
            String holder = draft.crewSlotIds().get(slot);
            Astronaut astronaut = find(session.state().getAstronauts(), holder);
            int seat = slot;
            String value = astronaut == null ? null
                : String.valueOf(Math.round(MissionStaffing.seatFit(astronaut, configuration, seat)));
            card.add(tile(configuration.crewSlots().get(slot), "Astronaut", true, null, "Seat fit",
                astronaut == null ? null : astronaut.getName(), value, used,
                () -> openPicker(true, seat))).padBottom(4f).row();
            if (astronaut != null) {
                card.add(ui.paragraph(seatSkills(astronaut, seat), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                    .padBottom(Theme.SPACE_2).row();
            }
        }
        return card;
    }

    /**
     * Lists the skills the steps lean on in a seat, with the holder's values.
     */
    private String seatSkills(Astronaut astronaut, int seat) {
        Set<AstronautSkill> skills = EnumSet.noneOf(AstronautSkill.class);
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
                    if (involvement.slot() == seat) {
                        skills.add(involvement.skill());
                    }
                }
            }
        }
        List<String> parts = new ArrayList<>();
        for (AstronautSkill skill : skills) {
            parts.add(CrewSkills.label(skill.name()) + " " + Math.round(astronaut.getSkill(skill)));
        }
        return "Seat fit weighs " + String.join(", ", parts) + " as the steps lean on them.";
    }

    private Table rocketCard(MissionDraft draft) {
        Table card = ui.card();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label("ROCKET", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        Label change = ui.label("Change in Rocket assignment", Theme.TextStyle.CAPTION, Theme.NAVY);
        change.setTouchable(Touchable.enabled);
        change.addListener(clicked(() -> game.setScreen(new MissionPlanScreen(game, configuration.id(),
            MissionPlanScreen.Tab.ROCKETS))));
        head.add(change);
        card.add(head).padBottom(Theme.SPACE_2).row();
        for (int launch = 0; launch < configuration.launches().size(); launch++) {
            String rocketId = draft.rocketIds().get(launch);
            Table row = new Table();
            if (rocketId == null) {
                row.add(ui.label("Launch " + (launch + 1) + " · no rocket can fly it", Theme.TextStyle.BODY_STRONG,
                    Theme.DANGER)).left();
            } else {
                MissionComponent rocket = session.catalog().missionComponent(rocketId).orElseThrow();
                Image glyph = new Image(ui.icon(SetSkills.icon(rocket.associatedSkill())), Scaling.fit);
                glyph.setColor(Theme.NAVY);
                row.add(glyph).size(14f).padRight(6f);
                row.add(ui.label("Launch " + (launch + 1) + " · " + ContentNames.of(rocketId),
                    Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
                double spare = rocket.payloadCapacity(configuration.launches().get(launch).launchType())
                    - MissionLaunch.payloadMass(configuration.launches().get(launch), session.catalog());
                row.add(ui.label(String.format(Locale.ROOT, "%.1f%% · %,.0f kg spare", session.state()
                    .getMissionComponents().get(rocketId).getCurrentReliability(), Math.max(0, spare)),
                    Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            }
            card.add(row).padBottom(4f).row();
        }
        return card;
    }

    private Table payCard(MissionDraft draft) {
        PlayerSnapshot state = session.state();
        MissionAssessment.Assessment odds = draft.assess(state, session.catalog());
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label("WITH THIS STAFF", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table stats = new Table();
        stats.left();
        stats.add(stat(MissionPlanScreen.pct(odds.completion()), "Completes", Theme.INK)).padRight(22f);
        if (Missions.crewCount(configuration) > 0) {
            stats.add(stat(MissionPlanScreen.pct(odds.crewLoss()), "Crew lost", Theme.GOLD_TEXT));
        }
        card.add(stats).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("Odds for the people in the slots now; an empty slot reads 0. Expert rescues aren't "
            + "counted.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();

        card.add(ui.label("CONFIRM & PAY", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        List<Programs.PriceLine> lines = new ArrayList<>();
        lines.add(new Programs.PriceLine(Missions.name(configuration) + " configuration", configuration.cost(), null));
        long total = configuration.cost();
        for (int launch = 0; launch < draft.rocketIds().size(); launch++) {
            String rocketId = draft.rocketIds().get(launch);
            if (rocketId != null) {
                long unit = session.catalog().missionComponent(rocketId).orElseThrow().unitCost();
                lines.add(new Programs.PriceLine(ContentNames.of(rocketId) + ", launch " + (launch + 1)
                    + ", unit cost", unit, null));
                total += unit;
            }
        }
        card.add(priceBreakdown(lines, total)).padBottom(4f).row();
        Table after = new Table();
        // Once launched, the state's funds already have the cost taken off.
        after.add(ui.label(launched == null ? "Funds after launch" : "Funds now", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).expandX().left();
        long left = launched == null ? state.getFunds() - total : state.getFunds();
        after.add(ui.label(money(left), Theme.TextStyle.CAPTION, left < 0 ? Theme.DANGER : Theme.INK_MUTED));
        card.add(after).padBottom(Theme.SPACE_4).row();

        if (launched != null) {
            card.add(ui.notice(Theme.NoticeKind.SUCCESS, "Scheduled.", Missions.name(configuration) + " flies at the "
                + "next turn. Until then you can scrub it in Mission Control for a full refund.")).padBottom(
                Theme.SPACE_3).row();
            card.add(actionButton("See it in Mission Control", Theme.ButtonKind.SECONDARY,
                () -> game.setScreen(new MissionsScreen(game, true)))).left().fill(false, false);
            return card;
        }
        LaunchRequest request = draft.request();
        MissionLaunchOutcome check = MissionLaunch.check(state, request, session.catalog());
        Table launchRow = new Table();
        launchRow.add(ui.paragraph(blockReason(check, total), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX()
            .fillX().left().padRight(Theme.SPACE_3);
        long price = total;
        TextButton launch = actionButton("Launch — " + money(price), Theme.ButtonKind.COMMIT,
            () -> confirmLaunch(draft, price));
        launch.setDisabled(!editable() || check != MissionLaunchOutcome.LAUNCHED);
        launchRow.add(launch);
        card.add(launchRow);
        return card;
    }

    private String blockReason(MissionLaunchOutcome check, long total) {
        if (!session.state().getOpenedProgramIds().contains(program.id())) {
            return "Open the program first.";
        }
        if (check == MissionLaunchOutcome.LAUNCHED) {
            return "Flies at the next turn.";
        }
        if (check == MissionLaunchOutcome.INSUFFICIENT_FUNDS) {
            return "Not enough funds: " + money(total - session.state().getFunds()) + " short.";
        }
        return Refusals.text(check.name());
    }

    private Table stat(String value, String label, Color color) {
        Table stat = new Table();
        stat.defaults().left();
        stat.add(ui.label(value, Theme.TextStyle.HEADING_LG, color)).row();
        stat.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED));
        return stat;
    }

    // ---- Mission control

    private Table controlCard(MissionDraft draft) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label("Mission control", Theme.TextStyle.HEADING_SM, Theme.INK)).left().padRight(Theme.SPACE_4);
        int used = configuration.usedControlSlots().size();
        head.add(ui.label(configuration.controlSlots().size() + " consoles, " + used + " used", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).expandX().left();
        card.add(head).padBottom(Theme.SPACE_3).row();
        Table grid = new Table();
        grid.top().left();
        PlayerSnapshot state = session.state();
        for (int slot = 0; slot < configuration.controlSlots().size(); slot++) {
            StaffingSlot staffing = configuration.controlSlots().get(slot);
            String holder = draft.controlSlotIds().get(slot);
            Personnel person = staffing.kind() == StaffingSlot.Kind.ASTRONAUT
                ? find(state.getAstronauts(), holder) : find(state.getMissionControlOfficers(), holder);
            String value = person == null ? null
                : String.valueOf(Math.round(MissionProgress.slotValue(state, staffing, holder)));
            int index = slot;
            grid.add(tile(staffing.title(), staffing.kind() == StaffingSlot.Kind.ASTRONAUT ? "Astronaut"
                    : "Flight controller", staffing.kind() == StaffingSlot.Kind.ASTRONAUT,
                    CrewSkills.icon(staffing.skill()), CrewSkills.label(staffing.skill()),
                    person == null ? null : person.getName(), value, configuration.usedControlSlots().contains(slot),
                    () -> openPicker(false, index)))
                .expandX().fillX().uniformX().top().pad(0f, 0f, 10f, slot % COLUMNS < COLUMNS - 1 ? 10f : 0f);
            if (slot % COLUMNS == COLUMNS - 1) {
                grid.row();
            }
        }
        for (int slot = configuration.controlSlots().size(); slot % COLUMNS != 0; slot++) {
            grid.add().expandX().fillX().uniformX();
        }
        card.add(grid).row();
        card.add(ui.paragraph("Any flight controller fits any controller slot; CAPCOM takes an astronaut who isn't in "
            + "the crew. A slot reads only its own skill (a FLIGHT slot reads the average of all five). Slots no step "
            + "uses stay empty.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(Theme.SPACE_2);
        return card;
    }

    private Table tile(String title, String type, boolean astronaut, String icon, String skill, String person,
                       String value, boolean used, Runnable open) {
        boolean nobody = used && person == null && candidates(astronaut ? StaffingSlot.Kind.ASTRONAUT
            : StaffingSlot.Kind.MISSION_CONTROL, null).isEmpty();
        SlotTile.State state = !used ? SlotTile.State.UNUSED : person != null ? SlotTile.State.FILLED
            : nobody ? SlotTile.State.NOBODY : SlotTile.State.EMPTY;
        Table tile = SlotTile.build(ui, title, type, astronaut, icon, skill, person, value, state);
        if (used && editable()) {
            tile.setTouchable(Touchable.enabled);
            tile.addListener(clicked(open));
        }
        return tile;
    }

    // ---- The slot picker

    private List<? extends Personnel> candidates(StaffingSlot.Kind kind, java.util.function.ToDoubleFunction<Personnel>
            value) {
        return draft().freeFor(kind, value == null ? person -> 0 : value, session.state());
    }

    /**
     * Opens the picker for a slot: the free people of its type, best at its skill first, plus
     * "Empty this slot".
     *
     * @param crew whether it's a crew seat (else a control slot)
     * @param slot the slot
     */
    public void openPicker(boolean crew, int slot) {
        MissionDraft draft = draft();
        PlayerSnapshot state = session.state();
        StaffingSlot.Kind kind;
        String title;
        String skillLabel;
        java.util.function.ToDoubleFunction<Personnel> value;
        String holder;
        if (crew) {
            kind = StaffingSlot.Kind.ASTRONAUT;
            title = configuration.crewSlots().get(slot);
            skillLabel = "Seat fit";
            value = person -> MissionStaffing.seatFit((Astronaut) person, configuration, slot);
            holder = draft.crewSlotIds().get(slot);
        } else {
            StaffingSlot staffing = configuration.controlSlots().get(slot);
            kind = staffing.kind();
            title = staffing.title();
            skillLabel = CrewSkills.label(staffing.skill());
            value = person -> MissionProgress.slotValue(state, staffing, person.getId());
            holder = draft.controlSlotIds().get(slot);
        }
        boolean astronaut = kind == StaffingSlot.Kind.ASTRONAUT;
        Table card = dialogCard(title, astronaut ? "Pick an astronaut" : "Pick a flight controller");
        card.add(ui.paragraph("Free people only: idle, available, and not in another slot of this mission. Sorted by "
            + skillLabel + ", the value the roll will read.", Theme.TextStyle.BODY, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_3).row();
        List<? extends Personnel> people = candidates(kind, value);
        Table list = new Table();
        list.top();
        list.defaults().growX();
        for (Personnel person : people) {
            Table row = new Table();
            row.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            row.pad(8f, 10f, 8f, 10f);
            row.add(ui.label(person.getName(), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
            row.add(ui.label(skillLabel, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(Theme.SPACE_2);
            double v = value.applyAsDouble(person);
            row.add(new SkillBar(ui, v)).width(120f).height(8f).padRight(Theme.SPACE_2);
            row.add(ui.label(String.valueOf(Math.round(v)), Theme.TextStyle.BODY_STRONG, Theme.NAVY)).width(28f);
            row.setTouchable(Touchable.enabled);
            row.addListener(clicked(() -> {
                if (crew) {
                    draft.setCrew(slot, person.getId());
                } else {
                    draft.setControl(slot, person.getId());
                }
                closeDialog();
                rebuild();
            }));
            list.add(row).padBottom(4f).row();
        }
        if (people.isEmpty()) {
            list.add(ui.notice(Theme.NoticeKind.WARNING, "Nobody is free.", astronaut
                ? "Every astronaut is busy or already on this mission. Hire more in the Astronaut Complex."
                : "Every flight controller is busy or already on this mission. Hire more in Mission Control."));
        }
        card.add(scrollPane(list)).maxHeight(420f).padBottom(Theme.SPACE_3).row();
        Table buttons = new Table();
        if (holder != null) {
            buttons.add(actionButton("Empty this slot", Theme.ButtonKind.SECONDARY, () -> {
                if (crew) {
                    draft.setCrew(slot, null);
                } else {
                    draft.setControl(slot, null);
                }
                closeDialog();
                rebuild();
            })).padRight(10f);
        }
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }

    // ---- Launch

    /**
     * Asks to launch, with the spec's wording.
     *
     * @param draft the plan
     * @param price what it costs
     */
    public void confirmLaunch(MissionDraft draft, long price) {
        Table card = dialogCard("Confirm", "Launch for " + money(price) + "?");
        List<String> crew = draft.crewSlotIds().stream().filter(Objects::nonNull)
            .map(id -> find(session.state().getAstronauts(), id)).filter(Objects::nonNull).map(Personnel::getName)
            .toList();
        card.add(ui.paragraph("It flies at the next turn. Scrubbing before then refunds in full."
            + (crew.isEmpty() ? "" : " Aboard: " + String.join(", ", crew) + "."), Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        MissionDraft sent = draft.copy();
        LaunchRequest request = draft.request();
        buttons.add(actionButton("Launch", Theme.ButtonKind.COMMIT, () -> submit(new LaunchMissionAction(
            request.missionConfigurationId(), request.crewSlotIds(), request.controlSlotIds(), request.rocketIds()),
            result -> {
                launched = sent;
                session.clearDraft(configuration.id());
                closeDialog();
                rebuild();
                toast("Scheduled: " + Missions.name(configuration) + " flies at the next turn.",
                    Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }

    /**
     * Shows the screen as it is after a launch went through, by launching the draft on the
     * session's own state without a server. For the offscreen screenshot tool.
     */
    public void previewLaunched() {
        MissionDraft draft = session.draft(configuration);
        MissionDraft sent = draft.copy();
        if (MissionLaunch.launch(session.state(), draft.request(), session.catalog()) == MissionLaunchOutcome.LAUNCHED) {
            launched = sent;
            session.clearDraft(configuration.id());
            refreshChrome();
            rebuild();
        }
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
