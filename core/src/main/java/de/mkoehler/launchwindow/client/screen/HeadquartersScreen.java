package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.model.Building;

import java.util.List;

/**
 * Headquarters' landing screen ({@code ui-spec.md} &sect;4.6): the building panel (the open-program
 * cap) and the two ways in: manage the open programs, or browse programs by celestial body.
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
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        int open = session.state().getOpenedProgramIds().size();
        content.add(buildingPanel(Building.HEADQUARTERS, "Headquarters", "Open programs",
            open + " / " + Programs.cap(session.state(), session.catalog()), "open-program cap"))
            .padBottom(Theme.SPACE_5).row();

        Table cards = new Table();
        cards.add(navCard("Currently open · " + open, "Manage open programs",
            "Upkeep, component reliability, missions in flight, and the next goal each open program can reach.",
            "Manage open programs", () -> game.setScreen(new OpenProgramsScreen(game))))
            .expandX().fillX().uniformX().top().padRight(Theme.SPACE_4);
        cards.add(navCard("By celestial body", "Browse programs",
            "Pick a destination, then a category, to see which programs are open, available, or still locked "
                + "behind a goal.", "Browse programs", () -> game.setScreen(new SelectBodyScreen(game))))
            .expandX().fillX().uniformX().top();
        content.add(cards);
    }

    private Table navCard(String eyebrow, String title, String body, String action, Runnable onClick) {
        Table card = ui.card();
        card.defaults().left().growX();
        card.add(ui.label(eyebrow, Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(Theme.SPACE_2).row();
        card.add(ui.label(title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(body, Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        card.add(actionButton(action, Theme.ButtonKind.PRIMARY, onClick)).left().fill(false, false);
        return card;
    }
}
