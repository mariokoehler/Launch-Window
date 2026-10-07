package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
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
            .expandX().fillX().uniformX().top().padRight(Theme.SPACE_4);
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
}
