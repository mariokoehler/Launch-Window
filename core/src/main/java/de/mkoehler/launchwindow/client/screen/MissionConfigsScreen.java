package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.rules.MissionPrestige;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mission configuration selection ({@code ui-spec.md} &sect;4.6.5): a card per configuration of a
 * program with its art, duration, the next flight's prestige, launches, cost and staffing.
 * Viewable when the program isn't open; then planning is read-only, with a way to open it.
 *
 * @author Mario Koehler
 */
public final class MissionConfigsScreen extends ChromeScreen {

    private static final float ART_ASPECT = 2.4f;
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
        List<Crumb> crumbs = new ArrayList<>();
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
        int index = 0;
        for (MissionConfiguration configuration : configurations) {
            grid.add(card(configuration)).expandX().fillX().uniformX().top()
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

    private Table card(MissionConfiguration configuration) {
        boolean open = session.state().getOpenedProgramIds().contains(program.id());
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        card.top();
        Drawable art = ui.croppedPicture("configurations/" + configuration.id() + ".jpg", ART_ASPECT);
        Image image = new Image(art == null ? ui.solid(Theme.NAVY) : art);
        if (!open) {
            image.setColor(1f, 1f, 1f, 0.6f);
        }
        card.add(image).growX().height(178f).pad(Theme.BORDER_THIN).row();

        Table body = new Table();
        body.top().left();
        body.defaults().left().growX();
        Table head = new Table();
        head.add(ui.label(Missions.name(configuration), Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        int flown = Missions.flown(configuration, session.state());
        head.add(ui.label(flown == 0 ? "Not flown" : "Flown " + flown + " ×", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).right();
        body.add(head).padBottom(10f).row();

        Table tags = new Table();
        tags.left();
        for (int i = 0; i < configuration.launches().size(); i++) {
            MissionConfiguration.Launch launch = configuration.launches().get(i);
            tags.add(tag(Missions.launchTag(launch.launchType()), false)).padRight(4f);
            if (launch.launchType().humanRated()) {
                tags.add(tag("Human-rated", true)).padRight(4f);
            }
        }
        if (configuration.launches().size() > 1) {
            tags.add(tag(configuration.launches().size() + " ×", false));
        }
        body.add(tags).padBottom(10f).row();

        Table stats = new Table();
        stats.left();
        stats.defaults().left().expandX().fillX().uniformX().padBottom(8f);
        String seasons = configuration.durationInSeasons() + (configuration.durationInSeasons() == 1 ? " season"
            : " seasons");
        int steps = configuration.seasons().stream().mapToInt(season -> season.steps().size()).sum();
        stat(stats, "Duration", seasons + " · " + steps + " steps");
        stat(stats, "Prestige", String.format(Locale.ROOT, "%,d", Missions.nextPrestige(configuration,
            session.state())));
        stat(stats, "Cost", money(configuration.cost()));
        stats.row();
        stat(stats, "Crew", String.valueOf(Missions.crewCount(configuration)));
        stat(stats, "Controllers", String.valueOf(Missions.controllerCount(configuration)));
        stat(stats, "CAPCOM", String.valueOf(Missions.capcomCount(configuration)));
        body.add(stats).row();
        body.add(ui.label(tierNote(flown), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(body).growX().pad(14f, 16f, 16f, 16f);
        card.setTouchable(Touchable.enabled);
        card.addListener(clicked(() -> game.setScreen(new MissionStepsScreen(game, configuration.id()))));
        return card;
    }

    private static String tierNote(int flown) {
        return switch (flown) {
            case 0 -> "Full prestige on the first flight.";
            case 1 -> MissionPrestige.SECOND_TIME_PERCENT + " % on the second flight.";
            default -> MissionPrestige.LATER_TIME_PERCENT + " % on every flight from now on.";
        };
    }

    private void stat(Table stats, String label, String value) {
        Table cell = new Table();
        cell.left();
        cell.defaults().left();
        cell.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(2f).row();
        cell.add(ui.label(value, Theme.TextStyle.BODY_SM, Theme.INK));
        stats.add(cell);
    }

    /**
     * Builds a small tag: a thin outline around a label, navy for emphasis.
     *
     * @param text the text
     * @param navy whether to draw it in navy
     * @return the tag
     */
    Table tag(String text, boolean navy) {
        Table tag = new Table();
        tag.setBackground(ui.rect(Theme.SURFACE_CARD, navy ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        tag.pad(2f, 6f, 2f, 6f);
        tag.add(ui.label(text, Theme.TextStyle.LABEL, navy ? Theme.NAVY : Theme.INK));
        return tag;
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
