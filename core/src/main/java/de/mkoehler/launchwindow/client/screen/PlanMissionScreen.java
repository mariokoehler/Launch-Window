package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;

import java.util.List;

/**
 * Plan a mission ({@code ui-spec.md} &sect;4.6, play-test request 2026-10-07): every mission
 * configuration of the programs open now, grouped by program, one click from Headquarters. A card
 * opens the mission plan on its assessment tab, as the path through a program's pages does, and
 * the plan and assembly then lead back here.
 *
 * @author Mario Koehler
 */
public final class PlanMissionScreen extends ChromeScreen {

    private static final int COLUMNS = 4;
    /** Shorter art than a program's own list, so more missions fit on screen. */
    private static final float ART_HEIGHT = 96f;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public PlanMissionScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.HEADQUARTERS, new Crumb("Plan a mission", null)),
            "Headquarters", "Plan a mission");
        rebuild();
    }

    @Override
    protected void goBack() {
        goHeadquarters();
    }

    /**
     * Returns the open programs that have missions to fly, in the content pack's order.
     *
     * @param host the screen asking
     * @return the programs
     */
    static List<Program> openWithMissions(ChromeScreen host) {
        return host.session.catalog().programs().stream()
            .filter(program -> host.session.state().getOpenedProgramIds().contains(program.id()))
            .filter(program -> !Missions.of(program, host.session.catalog()).isEmpty())
            .toList();
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        List<Program> programs = openWithMissions(this);
        if (programs.isEmpty()) {
            Table empty = new Table();
            empty.add(ui.notice(Theme.NoticeKind.INFO, "No program is open yet.", "Open one in Browse programs, "
                + "and its missions show up here.")).expandX().fillX().padRight(Theme.SPACE_4);
            empty.add(actionButton("Browse programs", Theme.ButtonKind.PRIMARY,
                () -> game.setScreen(new SelectBodyScreen(game))));
            content.add(empty).row();
            return;
        }
        Table list = new Table();
        list.top().left();
        list.defaults().growX();
        for (Program program : programs) {
            list.add(ui.label(ContentNames.of(program.id()), Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE))
                .left().padBottom(Theme.SPACE_2).row();
            Table grid = new Table();
            grid.top().left();
            int index = 0;
            for (MissionConfiguration configuration : Missions.of(program, session.catalog())) {
                grid.add(ConfigCards.card(this, configuration, true, () -> {
                    session.setPlanningFromList(true);
                    game.setScreen(new MissionPlanScreen(game, configuration.id(), MissionPlanScreen.Tab.ASSESSMENT));
                }, ART_HEIGHT)).expandX().fillX().uniformX().top()
                    .padRight(index % COLUMNS < COLUMNS - 1 ? Theme.SPACE_4 : 0f).padBottom(Theme.SPACE_4);
                if (++index % COLUMNS == 0) {
                    grid.row();
                }
            }
            for (int i = index % COLUMNS; i != 0 && i < COLUMNS; i++) {
                grid.add().expandX().fillX().uniformX();
            }
            list.add(grid).padBottom(Theme.SPACE_3).row();
        }
        ScrollPane scroll = scrollPane(list);
        content.add(scroll).expand().fill();
    }
}
