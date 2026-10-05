package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.CloseProgramAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.BodyMap;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Program details ({@code ui-spec.md} &sect;4.6.4): the program's art and upkeep, its required goals
 * as goal badges, the price breakdown, the components it grants with their reliability, and Open,
 * Close and Mission configurations. Reached from Manage open programs and from a body's programs;
 * Back and the breadcrumb lead to whichever the player came from.
 *
 * @author Mario Koehler
 */
public final class ProgramDetailsScreen extends ChromeScreen {

    private static final float ART_WIDTH = 620f;

    private final String programId;
    private final CelestialBody fromBody;

    /**
     * Builds the screen from the session.
     *
     * @param game      the game, in a session
     * @param programId the program
     * @param fromBody  the body whose program list led here, or {@code null} if Manage open programs did
     */
    public ProgramDetailsScreen(LaunchWindowGame game, String programId, CelestialBody fromBody) {
        super(game, crumbs(fromBody, programId), "Headquarters · "
                + game.session().catalog().category(game.session().catalog().program(programId).orElseThrow().categoryId())
                .map(category -> ContentNames.of(category.id(), category.name())).orElse(""), "Program details");
        this.programId = programId;
        this.fromBody = fromBody;
        rebuild();
    }

    private static List<Crumb> crumbs(CelestialBody fromBody, String programId) {
        List<Crumb> crumbs = new ArrayList<>();
        crumbs.add(Crumb.SPACE_COMPLEX);
        crumbs.add(Crumb.HEADQUARTERS);
        crumbs.add(fromBody == null
            ? new Crumb("Open programs", screen -> screen.game.setScreen(new OpenProgramsScreen(screen.game)))
            : new Crumb(BodyMap.name(fromBody), screen -> screen.game.setScreen(new BodyProgramsScreen(screen.game,
                fromBody))));
        crumbs.add(new Crumb(ContentNames.of(programId), null));
        return crumbs;
    }

    @Override
    protected void goBack() {
        game.setScreen(fromBody == null ? new OpenProgramsScreen(game) : new BodyProgramsScreen(game, fromBody));
    }

    private Program program() {
        return session.catalog().program(programId).orElseThrow();
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        Program program = program();
        PlayerSnapshot state = session.state();
        Programs.State programState = Programs.state(program, state);

        Table left = new Table();
        left.top();
        left.defaults().growX();
        Drawable art = ui.picture("programs/" + program.id() + ".jpg");
        Image image = art == null ? new Image(ui.solid(Theme.NAVY)) : new Image(art, Scaling.fill);
        if (programState == Programs.State.LOCKED) {
            image.setColor(0.6f, 0.6f, 0.6f, 0.6f);
        }
        left.add(image).size(ART_WIDTH, ART_WIDTH * 432f / 672f).padBottom(Theme.SPACE_3).row();
        Table upkeep = ui.card();
        upkeep.defaults().left().growX();
        upkeep.add(caption("Upkeep")).padBottom(Theme.SPACE_2).row();
        upkeep.add(ui.label(money(program.maintenanceCost()) + " / season", Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(Theme.SPACE_2).row();
        upkeep.add(ui.paragraph("Charged every season while the program is open. Each active component adds its own "
            + "upkeep.", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        left.add(upkeep);
        content.add(left).width(ART_WIDTH).top().padRight(Theme.SPACE_5);

        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        Table title = new Table();
        title.add(ui.label(ContentNames.of(program.id()), Theme.TextStyle.HEADING_LG, Theme.INK));
        String note = ContentNames.historyNote(program.id());
        if (note != null) {
            title.add(new HistoryNote(ui, note)).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2);
        }
        head.add(title).expandX().left();
        head.add(programStateMark(programState)).right();
        String description = ContentNames.description(program.id());
        card.add(head).padBottom(description == null ? 22f : Theme.SPACE_2).row();
        if (description != null) {
            card.add(ui.paragraph(description, Theme.TextStyle.BODY, Theme.INK)).width(760f).padBottom(22f).row();
        }

        card.add(caption("Required goals")).padBottom(10f).row();
        if (program.requiredGoalIds().isEmpty()) {
            card.add(ui.label("None. This program is open to your agency from the start.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).padBottom(22f).row();
        } else {
            Table badges = new Table();
            badges.left();
            for (String goal : program.requiredGoalIds()) {
                badges.add(goalBadge(goal, state)).width(110f).top().padRight(Theme.SPACE_5);
            }
            card.add(badges).padBottom(22f).row();
        }

        Programs.Price price = Programs.price(program, state, session.catalog());
        if (programState == Programs.State.OPEN) {
            card.add(caption("Price")).padBottom(6f).row();
            card.add(ui.label("Already open. Closing it gives no refund.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
                .padBottom(22f).row();
        } else {
            card.add(caption("Price to open")).padBottom(6f).row();
            card.add(priceBreakdown(price.lines(), price.total())).width(560f).padBottom(22f).row();
        }

        card.add(caption("Components it grants")).padBottom(Theme.SPACE_2).row();
        for (String componentId : program.requiredMissionComponentIds()) {
            card.add(componentBlock(componentId, programState, state)).width(760f).padBottom(Theme.SPACE_3).row();
        }

        card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).padTop(Theme.SPACE_3)
            .padBottom(Theme.SPACE_3).row();
        card.add(actions(program, programState, price));

        ScrollPane scroll = scrollPane(card);
        content.add(scroll).expand().fill().top();
    }

    private Table actions(Program program, Programs.State programState, Programs.Price price) {
        Table row = new Table();
        Table buttons = new Table();
        switch (programState) {
            case AVAILABLE -> buttons.add(actionButton("Open — " + money(price.total()), Theme.ButtonKind.PRIMARY,
                () -> tryOpen(program, price))).padRight(10f);
            case LOCKED -> {
                TextButton open = ui.button("Open", Theme.ButtonKind.PRIMARY);
                open.setDisabled(true);
                buttons.add(open).padRight(10f);
                buttons.add(ui.label("Achieve the required goals first.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                    .padRight(10f);
            }
            case OPEN -> buttons.add(actionButton("Close program", Theme.ButtonKind.DESTRUCTIVE,
                () -> tryClose(program))).padRight(10f);
        }
        buttons.add(actionButton("Mission configurations", Theme.ButtonKind.SECONDARY,
            () -> game.setScreen(new MissionConfigsScreen(game, programId))));
        row.add(buttons).expandX().left();
        row.add(ui.label("Headquarters: " + session.state().getOpenedProgramIds().size() + " / "
            + Programs.cap(session.state(), session.catalog()) + " programs open", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).right();
        return row;
    }

    private Table componentBlock(String componentId, Programs.State programState, PlayerSnapshot state) {
        MissionComponent component = session.catalog().missionComponent(componentId).orElseThrow();
        OwnedMissionComponent owned = state.getMissionComponents().get(componentId);
        Table block = new Table();
        block.defaults().growX().left();
        Table head = new Table();
        Table name = new Table();
        name.add(ui.label(ContentNames.of(componentId), Theme.TextStyle.BODY_STRONG, Theme.INK));
        String note = ContentNames.historyNote(componentId);
        if (note != null) {
            name.add(new HistoryNote(ui, note)).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2);
        }
        head.add(name).expandX().left();
        String status;
        if (owned == null) {
            status = "Not owned yet · creation " + money(component.creationCost());
        } else if (owned.isActive()) {
            status = programState == Programs.State.OPEN ? "Active" : "Already owned through another open program";
        } else {
            status = "Owned, inactive · kept from an earlier program";
        }
        head.add(ui.label(status, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        block.add(head).padBottom(4f).row();
        String description = ContentNames.description(componentId);
        if (description != null) {
            block.add(ui.paragraph(description, Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(6f).row();
        }
        if (owned != null) {
            ReliabilityBar bar = new ReliabilityBar(ui, owned.getCurrentReliability(), component.maxRdReliability(),
                component.maxReliability(), owned.getFloorReliability());
            if (!owned.isActive()) {
                bar.setColor(1f, 1f, 1f, 0.5f);
            }
            block.add(bar).height(14f).padBottom(4f).row();
            block.add(ui.label(String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f)", owned.getCurrentReliability(),
                component.maxRdReliability()), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        return block;
    }

    private Table goalBadge(String goalId, PlayerSnapshot state) {
        boolean achieved = state.getAchievedGoals().containsKey(goalId);
        boolean cancelled = state.getCancelledGoalIds().contains(goalId);
        Table badge = new Table();
        Stack patch = new Stack();
        patch.add(new Image(ui.goalPatch(achieved || cancelled), Scaling.fit));
        Image glyph = new Image(ui.icon(achieved || cancelled ? "check" : "prestige"), Scaling.fit);
        glyph.setColor(achieved || cancelled ? Theme.SUCCESS : Theme.NAVY);
        Table glyphHolder = new Table();
        glyphHolder.add(glyph).size(achieved || cancelled ? 26f : 22f);
        patch.add(glyphHolder);
        // No "First" ribbon: who reached a goal first is the race's standings, gap G3.
        badge.add(patch).size(64f).padBottom(Theme.SPACE_2).row();
        Label name = ui.paragraph(ContentNames.of(goalId), Theme.TextStyle.BODY_SM, Theme.INK);
        name.setAlignment(Align.center);
        badge.add(name).width(110f).row();
        // A cancelled goal was credited by a bigger one (GoalCascade), with its prestige; no turn is kept for it.
        String status = achieved ? "Achieved " + GameDate.ofTurn(Math.max(1, state.getAchievedGoals().get(goalId)))
            .label() : cancelled ? "Achieved" : "Not yet";
        Label statusLabel = ui.label(status, Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        statusLabel.setAlignment(Align.center);
        badge.add(statusLabel).width(110f);
        return badge;
    }

    private Label caption(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
    }

    /**
     * Opens the program after a confirmation, or says why it can't be opened (the Headquarters cap
     * or funds, checked here as the rule checks them; the server checks again).
     *
     * @param program the program
     * @param price   its price
     */
    public void tryOpen(Program program, Programs.Price price) {
        if (session.state().getOpenedProgramIds().size() >= Programs.cap(session.state(), session.catalog())) {
            toast(Refusals.text("HEADQUARTERS_FULL"), Theme.NoticeKind.INFO);
            return;
        }
        if (session.state().getFunds() < price.total()) {
            toast(Refusals.text("INSUFFICIENT_FUNDS"), Theme.NoticeKind.INFO);
            return;
        }
        String name = ContentNames.of(program.id());
        Table card = dialogCard("Confirm", "Open " + name + "?");
        card.add(ui.paragraph("Its upkeep of " + money(program.maintenanceCost()) + " is charged every season from the "
            + "next turn on.", Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(priceBreakdown(price.lines(), price.total())).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Open program", Theme.ButtonKind.PRIMARY, () -> submit(new OpenProgramAction(program.id()),
            result -> {
                closeDialog();
                // Straight on to what the player opened it for: its missions (play-test request).
                MissionConfigsScreen configs = new MissionConfigsScreen(game, program.id());
                game.setScreen(configs);
                configs.toast("Opened " + name + " — " + money(price.total()) + ". Pick a mission to plan.",
                    Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }

    /**
     * Closes the program after a confirmation, or says it can't while a mission uses it.
     *
     * @param program the program
     */
    public void tryClose(Program program) {
        if (Programs.missionsInFlight(program, session.state(), session.catalog()) > 0) {
            toast(Refusals.text("MISSIONS_IN_FLIGHT"), Theme.NoticeKind.INFO);
            return;
        }
        String name = ContentNames.of(program.id());
        Table card = dialogCard("Confirm", "Close " + name + "?");
        card.add(ui.paragraph("No refund. Its components go inactive and any SET researchers on them are released. "
            + "The components are kept, so reopening later costs only the opening cost.", Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Close program", Theme.ButtonKind.DESTRUCTIVE, () -> submit(
            new CloseProgramAction(program.id()), result -> {
                closeDialog();
                toast("Closed " + name + ". Its components are kept, inactive.", Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }
}
