package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Manage open programs ({@code ui-spec.md} &sect;4.6.1): one row per open program with its art,
 * upkeep, each component's reliability, missions in flight, and the next goal it can reach.
 *
 * @author Mario Koehler
 */
public final class OpenProgramsScreen extends ChromeScreen {

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public OpenProgramsScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.HEADQUARTERS,
            new Crumb("Open programs", null)), "Headquarters", "Open programs");
        rebuild();
    }

    @Override
    protected void goBack() {
        goHeadquarters();
    }

    @Override
    protected void rebuild() {
        titleActions.clearChildren();
        titleActions.add(ui.label(session.state().getOpenedProgramIds().size() + " of "
                + Programs.cap(session.state(), session.catalog()) + " open · Headquarters Level "
                + session.state().buildingLevel(Building.HEADQUARTERS), Theme.TextStyle.CAPTION, Theme.ON_PAGE_MUTED))
            .padRight(Theme.SPACE_4);
        titleActions.add(actionButton("Browse programs", Theme.ButtonKind.PRIMARY,
            () -> game.setScreen(new SelectBodyScreen(game))));

        content.clearChildren();
        content.top();
        List<Program> open = Programs.open(session.state(), session.catalog());
        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        if (open.isEmpty()) {
            list.add(ui.paragraph("No programs open yet. Browse programs to open your first.", Theme.TextStyle.BODY,
                Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (Program program : open) {
            list.add(row(program)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        ScrollPane scroll = scrollPane(list);
        card.add(scroll).expand().fill();
        content.add(card).expand().fill();
    }

    private Table row(Program program) {
        Table row = new Table();
        row.pad(Theme.SPACE_4);
        row.add(art(program.id(), 168f, 108f)).size(168f, 108f).padRight(Theme.SPACE_5);

        Table about = new Table();
        about.left();
        about.defaults().left().padBottom(6f);
        about.add(ui.label(ContentNames.of(program.id()), Theme.TextStyle.HEADING_SM, Theme.INK)).row();
        about.add(chip(session.catalog().category(program.categoryId()).map(c -> c.name()).orElse(""))).row();
        about.add(ui.label("Upkeep " + money(program.maintenanceCost()) + " / season", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED));
        row.add(about).width(220f).top().left().padRight(Theme.SPACE_5);

        Table components = new Table();
        components.top();
        components.defaults().growX().left();
        components.add(columnLabel("Components")).padBottom(6f).row();
        for (String componentId : program.requiredMissionComponentIds()) {
            MissionComponent component = session.catalog().missionComponent(componentId).orElseThrow();
            OwnedMissionComponent owned = session.state().getMissionComponents().get(componentId);
            Table head = new Table();
            head.add(ui.label(ContentNames.of(componentId), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            double current = owned == null ? 0 : owned.getCurrentReliability();
            head.add(ui.label(String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f)", current, component.maxRdReliability()),
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
            components.add(head).padBottom(4f).row();
            components.add(new ReliabilityBar(ui, current, component.maxRdReliability(), component.maxReliability(),
                owned == null ? 0 : owned.getFloorReliability())).height(14f).padBottom(6f).row();
        }
        row.add(components).expandX().fillX().top().padRight(Theme.SPACE_5);

        Table flight = new Table();
        flight.top().left();
        flight.defaults().left();
        flight.add(columnLabel("In flight")).padBottom(6f).row();
        int inFlight = Programs.missionsInFlight(program, session.state(), session.catalog());
        if (inFlight > 0) {
            flight.add(ui.badge(Theme.BadgeKind.ACTIVE, inFlight + (inFlight == 1 ? " mission" : " missions")));
        } else {
            flight.add(ui.label("None", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
        }
        row.add(flight).width(170f).top().padRight(Theme.SPACE_5);

        Table goal = new Table();
        goal.top().left();
        goal.defaults().left();
        goal.add(columnLabel("Next goal")).padBottom(6f).row();
        Optional<String> next = Programs.nextGoal(program, session.state(), session.catalog());
        Table goalRow = new Table();
        goalRow.add(new Image(ui.goalPatch(false), Scaling.fit)).size(28f).padRight(Theme.SPACE_2);
        Table goalText = new Table();
        goalText.left();
        goalText.defaults().left();
        goalText.add(ui.label(next.map(ContentNames::of).orElse("All reached"), Theme.TextStyle.BODY_SM, Theme.INK)).row();
        if (next.isPresent()) {
            long prestige = session.catalog().goal(next.get()).map(Goal::firstTimePrestige).orElse(0L);
            // "First time" needs the race's standings (gap G3), so the reward reads "up to".
            goalText.add(ui.label("Up to " + prestige + " prestige", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        goalRow.add(goalText);
        goal.add(goalRow);
        row.add(goal).width(240f).top().padRight(Theme.SPACE_5);

        row.add(actionButton("Details", Theme.ButtonKind.SECONDARY, () -> game.setScreen(
            new ProgramDetailsScreen(game, program.id(), null)))).top();
        return row;
    }

    private Label columnLabel(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
    }

    private Container<Label> chip(String text) {
        Container<Label> chip = new Container<>(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        chip.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        chip.pad(2f, Theme.SPACE_2, 2f, Theme.SPACE_2);
        return chip;
    }

    /**
     * Builds a program's art, cropped to fill, or a navy placeholder if the art isn't there.
     *
     * @param programId the program's id
     * @param width     the width it's shown at
     * @param height    the height it's shown at
     * @return the image
     */
    Image art(String programId, float width, float height) {
        Drawable art = ui.picture("programs/" + programId + ".jpg");
        return art == null ? new Image(ui.solid(Theme.NAVY)) : new Image(art, Scaling.fill);
    }
}
