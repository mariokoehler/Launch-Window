package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.client.view.Rockets;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.rules.RocketProgramOutcome;
import de.mkoehler.launchwindow.rules.RocketPrograms;

import java.util.List;
import java.util.Locale;

/**
 * Open Rocket Program ({@code ui-spec.md} &sect;4.5.1): the agency's rockets whose programs aren't
 * open, each with its class, payload to each destination, and costs. Opening charges the
 * creation cost, again on every reopening; a rocket whose class the VAB can't build yet is greyed
 * out. The checks are {@link RocketPrograms#checkOpen}'s, the server's own.
 *
 * @author Mario Koehler
 */
public final class OpenRocketScreen extends ChromeScreen {

    /** A greyed-out row's opacity, as in the mockup. */
    private static final float DISABLED_ALPHA = 0.55f;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public OpenRocketScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.VAB, new Crumb("Open Rocket Program", null)), "VAB",
            "Open Rocket Program");
        rebuild();
    }

    @Override
    protected void goBack() {
        goVab();
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        List<MissionComponent> rockets = Rockets.closed(session.state(), session.catalog());
        Table card = ui.card();
        card.pad(Theme.BORDER_EMPHASIS);
        card.top();
        Table list = new Table();
        list.top();
        list.defaults().growX();
        if (rockets.isEmpty()) {
            list.add(ui.paragraph("Every rocket design this agency has is already open.", Theme.TextStyle.BODY,
                Theme.INK_MUTED)).pad(Theme.SPACE_6);
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
        boolean buildable = Rockets.classAllowed(rocket, session.state());
        Table row = new Table();
        row.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        row.add(rocketIcon(rocket.associatedSkill())).size(40f).padRight(Theme.SPACE_4);

        Table about = new Table();
        about.left();
        about.defaults().left();
        Table name = new Table();
        name.add(ui.label(ContentNames.of(rocket.id()), Theme.TextStyle.HEADING_SM, Theme.INK));
        String note = ContentNames.historyNote(rocket.id());
        if (note != null) {
            name.add(new HistoryNote(ui, note)).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2);
        }
        about.add(name).padBottom(4f).row();
        Table chips = new Table();
        chips.add(chip(Rockets.className(rocket.rocket().rocketClass())));
        if (rocket.associatedSkill() == SetSkill.HUMAN_RATED_ROCKETS) {
            chips.add(chip("Human-rated")).padLeft(4f);
        }
        about.add(chips);
        row.add(about).width(150f).left().padRight(Theme.SPACE_4);

        Table payload = new Table();
        payload.left();
        String[][] stats = {{"Suborbital", kg(rocket.rocket().payloadToSuborbital())},
            {"LEO", kg(rocket.rocket().payloadToLeo())}, {"TLI", kg(rocket.rocket().payloadToTli())},
            {"TPI", kg(rocket.rocket().payloadToTpi())}};
        for (String[] stat : stats) {
            Table cell = new Table();
            cell.left();
            cell.defaults().left();
            cell.add(ui.label(stat[0].toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padBottom(2f).row();
            cell.add(ui.label(stat[1], Theme.TextStyle.BODY_SM, Theme.INK));
            payload.add(cell).width(90f).padRight(Theme.SPACE_4);
        }
        row.add(payload).expandX().left().padRight(Theme.SPACE_4);

        Table costs = new Table();
        costs.left();
        costs.defaults().left();
        costs.add(ui.label("Creation " + money(rocket.creationCost()), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padBottom(2f).row();
        costs.add(ui.label("Unit " + money(rocket.unitCost()) + " · Upkeep " + money(rocket.maintenanceCost())
            + "/season", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(costs).width(220f).left().padRight(Theme.SPACE_4);

        Table action = new Table();
        action.right();
        if (buildable) {
            action.add(actionButton("Open — " + money(rocket.creationCost()), Theme.ButtonKind.PRIMARY,
                () -> tryOpen(rocket))).right();
        } else {
            TextButton open = ui.button("Open", Theme.ButtonKind.SECONDARY);
            open.setDisabled(true);
            action.add(open).right().row();
            action.add(ui.label("Upgrade the VAB to build this rocket class.", Theme.TextStyle.CAPTION, Theme.DANGER))
                .right().padTop(4f);
            row.setColor(1f, 1f, 1f, DISABLED_ALPHA);
        }
        row.add(action).width(240f).right();
        return row;
    }

    private Image rocketIcon(SetSkill skill) {
        Image glyph = new Image(ui.icon(SetSkills.icon(skill)), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        return glyph;
    }

    private Table chip(String text) {
        Table chip = new Table();
        chip.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        chip.pad(2f, Theme.SPACE_2, 2f, Theme.SPACE_2);
        chip.add(ui.label(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return chip;
    }

    private static String kg(double amount) {
        return String.format(Locale.ROOT, "%,.0f kg", amount);
    }

    /**
     * Opens a rocket program after a confirmation, or says why it can't open (the VAB's cap or
     * class, or funds, checked as the server checks them; the server checks again).
     *
     * @param rocket the rocket
     */
    public void tryOpen(MissionComponent rocket) {
        RocketProgramOutcome check = RocketPrograms.checkOpen(session.state(), rocket.id(), session.catalog());
        if (check != RocketProgramOutcome.OPENED) {
            toast(Refusals.text(check.name()), Theme.NoticeKind.INFO);
            return;
        }
        String name = ContentNames.of(rocket.id());
        Table card = dialogCard("Confirm", "Open " + name + "?");
        card.add(ui.paragraph(Rockets.kept(rocket, session.state())
                ? "Still owned from before, with its reliability. Reopening charges the creation cost again."
                : "Reopening this rocket later charges the creation cost again.", Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(priceBreakdown(List.of(new Programs.PriceLine("Creation cost", rocket.creationCost(), null)),
            rocket.creationCost())).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(10f);
        buttons.add(actionButton("Open Program", Theme.ButtonKind.PRIMARY,
            () -> submit(new OpenRocketProgramAction(rocket.id()), result -> {
                closeDialog();
                toast("Opened " + name + " — " + money(rocket.creationCost()) + ".", Theme.NoticeKind.SUCCESS);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }
}
