package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.view.Concepts;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.client.view.Rockets;
import de.mkoehler.launchwindow.client.view.Tips;
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
    protected Help.Context helpContext() {
        return Help.Context.VAB;
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
        // The strategy BASPM never spelled out: open and research a small rocket before a big one.
        content.add(ui.notice(Theme.NoticeKind.INFO, "Related rockets share their reliability.",
            "A rocket starts with part of the reliability of related rockets you already own, as they are when "
                + "you open it. Big rockets are slow to research, so opening and researching a smaller one first "
                + "gives them a head start.")).growX().padBottom(Theme.SPACE_4).row();
        content.add(card).expand().fill();
    }

    private Table row(MissionComponent rocket) {
        boolean buildable = Rockets.classAllowed(rocket, session.state());
        Table row = new Table();
        row.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        addRocketPicture(row, rocket);

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
        String[][] stats = {
            {"Suborbital", kg(rocket.rocket().payloadToSuborbital()), "Suborbital: up and back down",
                "The most this rocket can lift on a hop into space that falls back without circling the Earth, "
                    + "like the first Mercury and Vostok tests."},
            {"LEO", kg(rocket.rocket().payloadToLeo()), "LEO: low Earth orbit",
                "The most this rocket can put into orbit a few hundred kilometres up, where satellites and the "
                    + "first crewed flights circled the Earth."},
            {"TLI", kg(rocket.rocket().payloadToTli()), "TLI: trans-lunar injection",
                "The most this rocket can send from Earth onto a path to the Moon."},
            {"TPI", kg(rocket.rocket().payloadToTpi()), "TPI: trans-planetary injection",
                "The most this rocket can send onto a path to another planet."}};
        for (String[] stat : stats) {
            Table cell = new Table();
            cell.left();
            cell.defaults().left();
            cell.add(ui.label(stat[0].toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .padBottom(2f).row();
            cell.add(ui.label(stat[1], Theme.TextStyle.BODY_SM, Theme.INK));
            ui.tooltip(cell, stat[2], stat[3]);
            payload.add(cell).width(84f).padRight(Theme.SPACE_3);
        }
        row.add(payload).expandX().left().padRight(Theme.SPACE_4);

        row.add(reliability(rocket)).width(250f).left().padRight(Theme.SPACE_4);

        Table costs = new Table();
        costs.left();
        costs.defaults().left();
        costs.add(tip(ui.label("Creation " + money(rocket.creationCost()) + " · once", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED), new Tips.Tip("Creation cost", "Paid once to open the program, and again if you "
                + "reopen it after closing it."))).padBottom(2f).row();
        costs.add(tip(ui.label("Unit " + money(rocket.unitCost()) + " · per launch", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED), new Tips.Tip("Unit cost", "Paid for every rocket a mission launches, on top of the "
                + "mission's own cost."))).padBottom(2f).row();
        costs.add(tip(ui.label("Upkeep " + money(rocket.maintenanceCost()) + " · per season", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED), new Tips.Tip("Upkeep", "Paid every season while the program is open.")));
        row.add(costs).width(190f).left().padRight(Theme.SPACE_4);

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
        }
        row.add(action).width(240f).right();
        Table block = new Table();
        block.add(row).growX().row();
        List<Concepts.Concept> concepts = Concepts.standard().forComponent(rocket.id());
        if (!concepts.isEmpty()) {
            // Six chips don't fit in the row, so they get a line of their own, under the name.
            Table line = new Table();
            line.left();
            line.add(ui.label("CONCEPTS", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(64f).padRight(14f);
            line.add(conceptChips(concepts, () -> new OpenRocketScreen(game))).growX();
            block.add(line).growX().padLeft(72f).padRight(Theme.SPACE_4).padBottom(14f);
        }
        if (!buildable) {
            block.setColor(1f, 1f, 1f, DISABLED_ALPHA);
        }
        return block;
    }

    /**
     * The reliability the rocket would start at if opened now (from the rules), where that comes
     * from, and how fast it researches.
     */
    private Table reliability(MissionComponent rocket) {
        double start = RocketPrograms.openingReliability(session.state(), rocket);
        double transfer = start - rocket.baselineReliability();
        List<String> sources = Rockets.transferSources(rocket, session.state(), session.catalog());
        Table column = new Table();
        column.left();
        column.defaults().left();
        column.add(ui.label("STARTS AT", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        column.add(ui.label(String.format(Locale.ROOT, "%.0f %% reliability", start), Theme.TextStyle.BODY_STRONG,
            Theme.INK)).padBottom(2f).row();
        String from;
        if (Rockets.kept(rocket, session.state())) {
            from = "Kept from when it was open";
        } else if (transfer >= 0.5) {
            from = String.format(Locale.ROOT, "+%.0f from related rockets", transfer);
        } else if (!sources.isEmpty()) {
            from = "Gains from " + String.join(", ", sources);
        } else {
            from = "Shares with no other rocket";
        }
        column.add(ui.label(from, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        column.add(ui.label("R&D pace: " + Rockets.rdPace(rocket), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        String detail = sources.isEmpty() ? "No other rocket passes reliability to this one."
            : "When it opens, it gets a share of each related rocket's reliability at that moment: "
                + String.join(", ", sources) + String.format(Locale.ROOT, ", up to +%.0f in all.",
                rocket.maxReliabilityTransfer());
        ui.tooltip(column, "Reliability on opening", detail + " R&D pace is how quickly engineers raise its "
            + "reliability compared with other rockets.");
        return column;
    }

    /**
     * After a rocket program opens: offers to staff its R&amp;D straight away, since a new rocket
     * starts with low reliability and engineers are what raise it.
     *
     * @param rocket the rocket just opened
     * @param name   its display name
     */
    public void offerResearch(MissionComponent rocket, String name) {
        Table card = dialogCard("Program opened", "Assign engineers to " + name + "?");
        card.add(ui.paragraph(String.format(Locale.ROOT, "%s starts at %.0f %% reliability. Engineers on its R&D "
                + "raise it every season; up to four can work on it at once.", name,
            RocketPrograms.openingReliability(session.state(), rocket)), Theme.TextStyle.BODY,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(actionButton("Later", Theme.ButtonKind.SECONDARY, () -> {
            closeDialog();
            toast("Opened " + name + " — " + money(rocket.creationCost()) + ".", Theme.NoticeKind.SUCCESS);
        })).padRight(10f);
        buttons.add(actionButton("Assign R&D", Theme.ButtonKind.PRIMARY,
            () -> game.setScreen(new RndBoardScreen(game, RndBoardScreen.Board.ROCKET, rocket.id()))));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
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
                offerResearch(rocket, name);
            })));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }
}
