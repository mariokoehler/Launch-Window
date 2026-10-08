package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.CloseRocketProgramAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Rockets;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.rules.RocketProgramOutcome;
import de.mkoehler.launchwindow.rules.RocketPrograms;

import java.util.List;
import java.util.Locale;

/**
 * Manage Rocket Programs ({@code ui-spec.md} &sect;4.5.2): the open rockets with their reliability,
 * upkeep, and the missions flying on them, a way to close one, and the way to Rocket R&amp;D.
 * Closing is refused while a mission uses the rocket ({@link RocketPrograms#inFlight}).
 *
 * @author Mario Koehler
 */
public final class RocketProgramsScreen extends ChromeScreen {

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public RocketProgramsScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.VAB, new Crumb("Manage Rocket Programs", null)), "VAB",
            "Manage Rocket Programs");
        titleActions.add(actionButton("Rocket R&D", Theme.ButtonKind.PRIMARY,
            () -> game.setScreen(new RndBoardScreen(game, RndBoardScreen.Board.ROCKET))));
        rebuild();
    }

    @Override
    protected void goBack() {
        goVab();
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.VAB;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        List<MissionComponent> rockets = Rockets.open(session.state(), session.catalog());
        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        if (rockets.isEmpty()) {
            list.add(ui.paragraph("No rocket programs open yet. Open one to fly your first mission.",
                Theme.TextStyle.BODY, Theme.INK_MUTED)).pad(Theme.SPACE_6);
        }
        for (MissionComponent rocket : rockets) {
            list.add(row(rocket)).row();
            list.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        ScrollPane scroll = scrollPane(list);
        card.add(scroll).expand().fill();
        content.add(card).expand().fill();
    }

    private Table row(MissionComponent rocket) {
        OwnedMissionComponent owned = session.state().getMissionComponents().get(rocket.id());
        Table row = new Table();
        row.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        Image glyph = new Image(ui.icon(SetSkills.icon(rocket.associatedSkill())), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        row.add(glyph).size(40f).padRight(Theme.SPACE_4);

        Table about = new Table();
        about.left();
        about.defaults().left();
        about.add(ui.label(ContentNames.of(rocket.id()), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        about.add(chip(Rockets.className(rocket.rocket().rocketClass())));
        row.add(about).width(150f).left().padRight(Theme.SPACE_4);

        Table reliability = new Table();
        reliability.defaults().growX().left();
        reliability.add(new ReliabilityBar(ui, owned.getCurrentReliability(), rocket.maxRdReliability(),
            rocket.maxReliability(), owned.getFloorReliability())).height(14f).padBottom(4f).row();
        reliability.add(ui.label(String.format(Locale.ROOT, "%.1f%% (R&D cap %.1f)", owned.getCurrentReliability(),
            rocket.maxRdReliability()), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(reliability).expandX().fillX().padRight(Theme.SPACE_5);

        Table upkeep = new Table();
        upkeep.left();
        upkeep.defaults().left();
        upkeep.add(ui.label("UPKEEP", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        upkeep.add(ui.label(money(rocket.maintenanceCost()) + "/season", Theme.TextStyle.BODY_SM, Theme.INK));
        row.add(upkeep).width(150f).left().padRight(Theme.SPACE_4);

        Table flight = new Table();
        flight.left();
        List<String> missions = Rockets.missionsUsing(rocket.id(), session.state());
        if (!missions.isEmpty()) {
            flight.add(ui.badge(Theme.BadgeKind.ACTIVE, "On " + ContentNames.of(missions.get(0))
                + (missions.size() > 1 ? " +" + (missions.size() - 1) : ""))).left();
        }
        row.add(flight).width(220f).left().padRight(Theme.SPACE_4);

        TextButton close;
        if (missions.isEmpty()) {
            close = actionButton("Close", Theme.ButtonKind.DESTRUCTIVE, () -> confirmClose(rocket));
        } else {
            close = ui.button("Close", Theme.ButtonKind.SECONDARY);
            close.setDisabled(true);
            close.addListener(clicked(() -> toast(Refusals.text(RocketProgramOutcome.MISSIONS_IN_FLIGHT.name()),
                Theme.NoticeKind.INFO)));
        }
        row.add(close).width(120f).right();
        return row;
    }

    private Table chip(String text) {
        Table chip = new Table();
        chip.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        chip.pad(2f, Theme.SPACE_2, 2f, Theme.SPACE_2);
        chip.add(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return chip;
    }

    /**
     * Asks to close a rocket program.
     *
     * @param rocket the rocket
     */
    public void confirmClose(MissionComponent rocket) {
        String name = ContentNames.of(rocket.id());
        Table card = dialogCard("Confirm", "Close " + name + "?");
        card.add(ui.paragraph("No refund. It's kept with its current reliability and no upkeep while closed; "
            + "reopening it later costs the creation cost again. Its researchers are released.", Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Close", Theme.ButtonKind.DESTRUCTIVE,
            () -> submit(new CloseRocketProgramAction(rocket.id()), result -> {
                closeDialog();
                toast("Closed " + name + ".", Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }
}
