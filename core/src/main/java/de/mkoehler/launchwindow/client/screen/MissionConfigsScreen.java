package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Mission configuration selection ({@code ui-spec.md} &sect;4.6.5): a card per configuration of a
 * program with its art, duration, the next flight's prestige, launches, cost and staffing.
 * Viewable when the program isn't open; then planning is read-only, with a way to open it.
 *
 * @author Mario Koehler
 */
public final class MissionConfigsScreen extends ChromeScreen {

    private static final int COLUMNS = 4;

    private final Program program;

    /**
     * Builds the screen from the session.
     *
     * @param game      the game, in a session
     * @param programId the program
     */
    public MissionConfigsScreen(LaunchWindowGame game, String programId) {
        super(game, crumbs(programId, null, null), "Headquarters · " + ContentNames.of(programId),
            "Mission configurations");
        this.program = session.catalog().program(programId).orElseThrow();
        rebuild();
    }

    /**
     * The planning screens' breadcrumb: Space Complex, Headquarters, the program, its
     * configurations, then the configuration and a last step, when given.
     *
     * @param programId     the program
     * @param configuration the configuration, or {@code null}
     * @param last          the last step's label ("Plan", "Assemble"), or {@code null}
     * @return the crumbs
     */
    static List<Crumb> crumbs(String programId, MissionConfiguration configuration, String last) {
        return crumbs(null, programId, configuration, last);
    }

    /**
     * As above, but through "Plan a mission" when the player came from Headquarters' list
     * ({@link de.mkoehler.launchwindow.client.ClientSession#planningFromList}).
     *
     * @param session       the session, or {@code null} for the path through the program
     * @param programId     the program
     * @param configuration the configuration, or {@code null}
     * @param last          the last step's label ("Plan", "Assemble"), or {@code null}
     * @return the crumbs
     */
    static List<Crumb> crumbs(de.mkoehler.launchwindow.client.ClientSession session, String programId,
            MissionConfiguration configuration, String last) {
        List<Crumb> crumbs = new ArrayList<>();
        if (session != null && session.planningFromList() && configuration != null && last != null) {
            crumbs.add(Crumb.SPACE_COMPLEX);
            crumbs.add(Crumb.HEADQUARTERS);
            crumbs.add(new Crumb("Plan a mission", screen -> screen.game.setScreen(new PlanMissionScreen(screen.game))));
            // Named with its program: the list mixes programs, and two can share a configuration's name.
            crumbs.add(new Crumb(ContentNames.of(programId) + " · " + Missions.name(configuration), "Plan".equals(last)
                ? screen -> screen.game.setScreen(new MissionStepsScreen(screen.game, configuration.id()))
                : screen -> screen.game.setScreen(new MissionPlanScreen(screen.game, configuration.id(),
                    MissionPlanScreen.Tab.ASSESSMENT))));
            crumbs.add(new Crumb(last, null));
            return crumbs;
        }
        crumbs.add(Crumb.SPACE_COMPLEX);
        crumbs.add(Crumb.HEADQUARTERS);
        crumbs.add(new Crumb(ContentNames.of(programId),
            screen -> screen.game.setScreen(new ProgramDetailsScreen(screen.game, programId, null))));
        crumbs.add(new Crumb("Mission configurations", configuration == null ? null
            : screen -> screen.game.setScreen(new MissionConfigsScreen(screen.game, programId))));
        if (configuration != null) {
            String name = Missions.name(configuration);
            if (last == null) {
                crumbs.add(new Crumb(name, null));
            } else if ("Plan".equals(last)) {
                crumbs.add(new Crumb(name,
                    screen -> screen.game.setScreen(new MissionStepsScreen(screen.game, configuration.id()))));
                crumbs.add(new Crumb(last, null));
            } else {
                crumbs.add(new Crumb(name, screen -> screen.game.setScreen(
                    new MissionPlanScreen(screen.game, configuration.id(), MissionPlanScreen.Tab.ASSESSMENT))));
                crumbs.add(new Crumb(last, null));
            }
        }
        return crumbs;
    }

    @Override
    protected void goBack() {
        game.setScreen(new ProgramDetailsScreen(game, program.id(), null));
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.PROGRAM;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        if (!session.state().getOpenedProgramIds().contains(program.id())) {
            content.add(readOnlyNotice(this, program, "You can study its missions, but planning stays read-only "
                + "until you open the program.")).padBottom(Theme.SPACE_4).row();
        }
        Table grid = new Table();
        grid.top().left();
        List<MissionConfiguration> configurations = Missions.of(program, session.catalog());
        boolean open = session.state().getOpenedProgramIds().contains(program.id());
        int index = 0;
        for (MissionConfiguration configuration : configurations) {
            grid.add(ConfigCards.card(this, configuration, open,
                () -> game.setScreen(new MissionStepsScreen(game, configuration.id())))).expandX().fillX().uniformX().top()
                .padRight(index % COLUMNS < COLUMNS - 1 ? Theme.SPACE_4 : 0f).padBottom(Theme.SPACE_4);
            if (++index % COLUMNS == 0) {
                grid.row();
            }
        }
        grid.add(legend()).expandX().fillX().uniformX().top();
        for (int i = (index + 1) % COLUMNS; i != 0 && i < COLUMNS; i++) {
            grid.add().expandX().fillX().uniformX();
        }
        ScrollPane scroll = scrollPane(grid);
        content.add(scroll).expand().fill();
    }

    /**
     * Says the program isn't open, with the way to open it: planning stays read-only until then.
     *
     * @param host    the screen it sits on
     * @param program the program
     * @param text    what stays read-only
     * @return the row
     */
    static Table readOnlyNotice(ChromeScreen host, Program program, String text) {
        Table row = new Table();
        row.add(host.ui.notice(Theme.NoticeKind.INFO, ContentNames.of(program.id()) + " isn't open.", text))
            .expandX().fillX().padRight(Theme.SPACE_4);
        long price = Programs.price(program, host.session.state(), host.session.catalog()).total();
        row.add(host.actionButton("Open " + ContentNames.of(program.id()) + " — " + money(price),
            Theme.ButtonKind.PRIMARY, () -> host.game.setScreen(new ProgramDetailsScreen(host.game, program.id(),
                null))));
        return row;
    }

    private Table legend() {
        Table legend = new Table();
        legend.setBackground(ui.rect(new Color(1f, 1f, 1f, 0.08f), Theme.ON_PAGE, Theme.BORDER_THIN));
        legend.pad(Theme.SPACE_4);
        legend.top().left();
        legend.defaults().left().growX().padBottom(Theme.SPACE_2);
        legend.add(ui.label("HOW PRESTIGE WORKS", Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE)).row();
        for (String line : new String[] {
            "A configuration pays its full prestige the first time you fly it, 50 % the second time, and 30 % "
                + "after that.",
            "Cost is the configuration's own. Each launch adds the unit cost of the rocket you assign to it.",
            "Crewed missions need a human-rated rocket and a CAPCOM, who is an astronaut."}) {
            Label label = ui.paragraph(line, Theme.TextStyle.BODY_SM, Theme.ON_PAGE_STRONG);
            legend.add(label).row();
        }
        return legend;
    }
}
