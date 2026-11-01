package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContractsView;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.model.Building;

import java.util.List;

/**
 * Headquarters' landing screen ({@code ui-spec.md} &sect;4.6): the building panel (the open-program
 * cap) and the three ways in: plan a mission straight away, manage the open programs, or browse
 * programs by celestial body.
 *
 * @author Mario Koehler
 */
public final class HeadquartersScreen extends ChromeScreen {

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public HeadquartersScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.HEADQUARTERS), "Headquarters",
            "Programs and mission planning");
        rebuild();
    }

    @Override
    protected void goHeadquarters() {
        // Already here.
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.HEADQUARTERS;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        int open = session.state().getOpenedProgramIds().size();
        content.add(buildingPanel(Building.HEADQUARTERS, "Headquarters", "Open programs",
            open + " / " + Programs.cap(session.state(), session.catalog()), "open-program cap"))
            .padBottom(Theme.SPACE_5).row();

        Table cards = new Table();
        long missions = PlanMissionScreen.openWithMissions(this).stream()
            .mapToLong(program -> de.mkoehler.launchwindow.client.view.Missions.of(program, session.catalog()).size())
            .sum();
        cards.add(navCard("Ready to plan · " + missions + (missions == 1 ? " mission" : " missions"), "Plan a mission",
            "Every mission of the programs open now, in one list. Pick one to assess its odds, choose rockets, and "
                + "assemble the crew.", "Plan a mission", () -> game.setScreen(new PlanMissionScreen(game))))
            .expandX().fill().uniformX().padRight(Theme.SPACE_4);
        cards.add(navCard("Currently open · " + open, "Manage open programs",
            "Upkeep, component reliability, missions in flight, and the next goal each open program can reach.",
            "Manage open programs", () -> game.setScreen(new OpenProgramsScreen(game))))
            .expandX().fill().uniformX().padRight(Theme.SPACE_4);
        cards.add(navCard("By celestial body", "Browse programs",
            "Pick a destination, then a category, to see which programs are open, available, or still locked "
                + "behind a goal.", "Browse programs", () -> game.setScreen(new SelectBodyScreen(game))))
            .expandX().fill().uniformX().padRight(Theme.SPACE_4);
        cards.add(contractsCard()).expandX().fill().uniformX();
        content.add(cards);
    }

    /**
     * The fourth card, Contracts ({@code ui-spec.md} &sect;4.17): the board's count, and a state row for
     * new offers (gold), an application, or the active contract.
     */
    private Table contractsCard() {
        String[] state = ContractsView.card(session.state(), session.catalog(), session.openTurn());
        Table card = ui.card();
        card.defaults().left().growX();
        card.add(ui.label(state[0].toUpperCase(java.util.Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY))
            .padBottom(Theme.SPACE_2).row();
        card.add(ui.label("Contracts", Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("Clients pay you to fly again what you've already flown: money, not prestige. "
            + "Apply for one a turn.", Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        boolean gold = !state[2].isEmpty();
        Table row = new Table();
        row.setBackground(gold ? ui.rect(new com.badlogic.gdx.graphics.Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.12f),
            Theme.GOLD, Theme.BORDER_THIN) : ui.rect(new com.badlogic.gdx.graphics.Color(Theme.SURFACE_CARD)
            .lerp(Theme.NAVY, 0.04f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        row.pad(8f, 10f, 8f, 10f);
        row.left();
        if (gold) {
            row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(ui.circle(Theme.GOLD, null, 0f, 8))).size(8f)
                .padRight(Theme.SPACE_2);
        }
        row.add(ui.paragraph(state[1], Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
        card.add(row).padBottom(Theme.SPACE_3).row();
        card.add(actionButton("Open the board", Theme.ButtonKind.PRIMARY, () -> game.setScreen(new ContractsScreen(game))))
            .expandY().bottom().left().fill(false, false);
        return card;
    }
}
