package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Rockets;
import de.mkoehler.launchwindow.content.RocketClass;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.rules.RocketPrograms;

import java.util.List;

/**
 * The VAB's landing screen ({@code ui-spec.md} &sect;4.5): the building panel, whose level caps
 * the open rocket programs and sets the largest rocket class, and the way to open and manage
 * rocket programs.
 *
 * @author Mario Koehler
 */
public final class VabScreen extends ChromeScreen {

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public VabScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("VAB", null)), "VAB", "Vehicle Assembly Building");
        rebuild();
    }

    @Override
    protected void goVab() {
        // Already here.
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.VAB;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        int level = session.state().buildingLevel(Building.VEHICLE_ASSEMBLY);
        int open = RocketPrograms.openCount(session.state(), session.catalog());
        content.add(buildingPanel(Building.VEHICLE_ASSEMBLY, "the VAB", "Rocket programs",
            open + " / " + Rockets.cap(session.state(), session.catalog()), "rocket-program cap",
            List.<String[]>of(new String[] {"Rocket class", Rockets.classes(level)}), next -> {
                RocketClass unlocked = Rockets.unlockedAt(next);
                return unlocked == null ? null : "the " + Rockets.className(unlocked) + " class";
            })).padBottom(Theme.SPACE_5).row();

        Table cards = new Table();
        cards.add(navCard("Rockets not yet open", "Open Rocket Program",
            "Choose from the agency's rocket designs that aren't open yet. Opening charges the creation cost, "
                + "again on every reopening.", "Open Rocket Program",
            () -> game.setScreen(new OpenRocketScreen(game))))
            .expandX().fill().uniformX().padRight(Theme.SPACE_4);
        cards.add(navCard("Currently open · " + open, "Manage Rocket Programs",
            "Reliability, upkeep, and in-flight missions for every rocket this agency has open. Also reaches "
                + "Rocket R&D.", "Manage Rocket Programs", () -> game.setScreen(new RocketProgramsScreen(game))))
            .expandX().fill().uniformX();
        content.add(cards);
    }
}
