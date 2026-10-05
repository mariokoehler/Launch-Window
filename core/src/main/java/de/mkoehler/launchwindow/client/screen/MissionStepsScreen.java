package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.CrewSkills;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.rules.GoalCascade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mission configuration details and its steps ({@code ui-spec.md} &sect;4.6.6): a summary, the goal
 * dependencies and boosts, and every step season by season with its component, console and crew
 * shares, crew risk, prestige share, and the goals it reaches (with what they also credit).
 *
 * @author Mario Koehler
 */
public final class MissionStepsScreen extends ChromeScreen {

    /** The width of the summary card's description: the 420 px column less the card's padding. */
    private static final float SUMMARY_TEXT_WIDTH = 384f;

    private final MissionConfiguration configuration;

    /**
     * Builds the screen from the session.
     *
     * @param game            the game, in a session
     * @param configurationId the configuration
     */
    public MissionStepsScreen(LaunchWindowGame game, String configurationId) {
        this(game, game.session().catalog().missionConfiguration(configurationId).orElseThrow());
    }

    private MissionStepsScreen(LaunchWindowGame game, MissionConfiguration configuration) {
        super(game, MissionConfigsScreen.crumbs(configuration.programId(), configuration, null),
            "Headquarters · " + ContentNames.of(configuration.programId()),
            Missions.name(configuration) + ": steps and goals");
        this.configuration = configuration;
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(new MissionConfigsScreen(game, configuration.programId()));
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        Drawable art = ui.croppedPicture("configurations/" + configuration.id() + ".jpg", 420f / 236f);
        left.add(new Image(art == null ? ui.solid(Theme.NAVY) : art)).height(236f).row();
        left.add(summary()).row();
        left.add(modifiers());
        content.add(scrollPane(left)).width(420f).top().fillY().padRight(Theme.SPACE_5);

        Table right = new Table();
        right.top();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label("Mission steps", Theme.TextStyle.HEADING_SM, Theme.INK)).left().padRight(Theme.SPACE_4);
        head.add(ui.paragraph("Shares are each step's weighting. Components set the odds, crew skills blend in on "
            + "well-built hardware, and controllers save a failing roll.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .expandX().fillX();
        card.add(head).padBottom(Theme.SPACE_3).row();
        card.add(stepsGrid());
        right.add(scrollPane(card)).expand().fill().row();

        Table actions = new Table();
        long deduction = configuration.seasons().stream().flatMap(season -> season.steps().stream())
            .mapToLong(MissionStep::failurePrestigeDeduction).max().orElse(0);
        actions.add(ui.label("A failed step costs up to " + deduction + " prestige.", Theme.TextStyle.CAPTION,
            Theme.ON_PAGE_MUTED)).expandX().left();
        actions.add(onClick(ui.pageButton("All configurations"),
            () -> game.setScreen(new MissionConfigsScreen(game, configuration.programId())))).padRight(Theme.SPACE_3);
        actions.add(actionButton("Plan this mission", Theme.ButtonKind.PRIMARY, () -> game.setScreen(
            new MissionPlanScreen(game, configuration.id(), MissionPlanScreen.Tab.ASSESSMENT))));
        right.add(actions).growX().padTop(Theme.SPACE_3);
        content.add(right).expand().fill();
    }

    private Table summary() {
        Table card = ui.card();
        card.defaults().left();
        Table head = new Table();
        Table title = new Table();
        title.add(ui.label(Missions.name(configuration), Theme.TextStyle.HEADING_SM, Theme.INK));
        String note = ContentNames.historyNote(configuration.id());
        if (note != null) {
            title.add(new HistoryNote(ui, note)).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2);
        }
        head.add(title).expandX().left();
        int flown = Missions.flown(configuration, session.state());
        head.add(ui.label(flown == 0 ? "Not flown yet" : "Flown " + flown + " ×", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED));
        card.add(head).growX().colspan(2).padBottom(Theme.SPACE_3).row();
        String description = ContentNames.description(configuration.id());
        if (description != null) {
            card.add(ui.paragraph(description, Theme.TextStyle.BODY_SM, Theme.INK)).width(SUMMARY_TEXT_WIDTH).colspan(2)
                .padBottom(Theme.SPACE_3).row();
        }
        String rockets = configuration.launches().size() == 1 ? "the rocket" : "the rockets";
        kv(card, "Cost", money(configuration.cost()) + " + " + rockets);
        kv(card, "Prestige", String.format(Locale.ROOT, "%,d", Missions.nextPrestige(configuration, session.state()))
            + (flown == 0 ? " on the first flight" : " on the next flight"));
        int steps = configuration.seasons().stream().mapToInt(season -> season.steps().size()).sum();
        kv(card, "Duration", configuration.durationInSeasons() + (configuration.durationInSeasons() == 1 ? " season, "
            : " seasons, ") + steps + " steps");
        for (int i = 0; i < configuration.launches().size(); i++) {
            MissionConfiguration.Launch launch = configuration.launches().get(i);
            kv(card, "Launch " + (i + 1), Missions.launchLabel(launch.launchType()) + ". Payload: "
                + Missions.payload(launch, session.catalog()) + ".");
        }
        kv(card, "Staffing", staffing());
        boolean crewed = Missions.crewCount(configuration) > 0;
        // A mission can only be called off between its seasons (MissionCancellation), so a one-season
        // flight never can; and the prestige figure is a loss, never a reward (play-test confusion).
        String scrub = "Scrubbing before launch refunds in full.";
        String onceFlying;
        if (configuration.durationInSeasons() == 1) {
            onceFlying = scrub + " It flies in a single season, so once launched it runs to the end.";
        } else if (crewed) {
            onceFlying = scrub + " Crewed, so once flying it can't be called off.";
        } else {
            onceFlying = scrub + String.format(Locale.ROOT, " Between seasons it can be called off: %d %% of the "
                + "launch cost back, but you lose %,d prestige. Prestige already earned is kept.",
                configuration.cancellationRefundPercent(), configuration.cancellationPrestigeDeduction());
        }
        kv(card, "Once flying", onceFlying);
        return card;
    }

    private String staffing() {
        int crew = Missions.crewCount(configuration);
        int controllers = Missions.controllerCount(configuration);
        int capcom = Missions.capcomCount(configuration);
        List<String> parts = new ArrayList<>();
        if (crew > 0) {
            parts.add(crew + (crew == 1 ? " astronaut" : " astronauts"));
        }
        parts.add(controllers + (controllers == 1 ? " flight controller" : " flight controllers"));
        if (capcom > 0) {
            parts.add(capcom + " CAPCOM");
        }
        List<String> unused = new ArrayList<>();
        for (int slot = 0; slot < configuration.controlSlots().size(); slot++) {
            if (!configuration.usedControlSlots().contains(slot)) {
                unused.add(configuration.controlSlots().get(slot).title());
            }
        }
        String text = String.join(", ", parts) + ".";
        if (unused.size() == 1) {
            text += " The " + unused.get(0) + " slot isn't used.";
        } else if (!unused.isEmpty()) {
            text += " The " + String.join(", ", unused) + " slots aren't used.";
        }
        return text;
    }

    private void kv(Table card, String key, String value) {
        card.add(ui.label(key.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(112f).top()
            .padTop(3f).padBottom(6f).padRight(10f);
        card.add(ui.paragraph(value, Theme.TextStyle.BODY_SM, Theme.INK)).growX().padBottom(6f).row();
    }

    private Table modifiers() {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label("GOAL DEPENDENCIES", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        card.add(ui.paragraph("Penalties on a component's reliability until you reach the goal.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        if (configuration.goalDependencies().isEmpty()) {
            card.add(ui.label("None for this configuration.", Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(
                Theme.SPACE_3).row();
        }
        for (MissionConfiguration.GoalModifier dependency : configuration.goalDependencies()) {
            boolean reached = session.state().hasAchievedOrCancelledGoal(dependency.goalId());
            card.add(modifierRow(dependency, reached, reached ? "Reached · no penalty"
                : "Not reached yet · " + ContentNames.of(dependency.componentId()), reached ? "—"
                : "−" + fmt(dependency.amount()), !reached)).row();
        }
        card.add(ui.label("GOAL BOOSTS", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(Theme.SPACE_3)
            .padBottom(Theme.SPACE_2).row();
        if (configuration.goalBoosts().isEmpty()) {
            card.add(ui.label("None for this configuration.", Theme.TextStyle.BODY_SM, Theme.INK)).row();
        }
        for (MissionConfiguration.GoalModifier boost : configuration.goalBoosts()) {
            boolean reached = session.state().hasAchievedOrCancelledGoal(boost.goalId());
            card.add(modifierRow(boost, reached, (reached ? "Reached · " : "Once reached · ")
                + ContentNames.of(boost.componentId()), "+" + fmt(boost.amount()), false)).row();
        }
        return card;
    }

    private Table modifierRow(MissionConfiguration.GoalModifier modifier, boolean reached, String status,
                              String amount, boolean open) {
        Table row = new Table();
        row.pad(8f, 0f, 8f, 0f);
        Image mark = new Image(ui.icon(reached ? "check" : "lock"), Scaling.fit);
        mark.setColor(reached ? Theme.SUCCESS : Theme.GOLD_TEXT);
        row.add(mark).size(14f).top().padRight(8f);
        Table text = new Table();
        text.left();
        text.defaults().left();
        text.add(ui.label(ContentNames.of(modifier.goalId()), Theme.TextStyle.BODY_SM, Theme.INK)).row();
        text.add(ui.label(status, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(text).expandX().fillX();
        row.add(ui.label(amount, Theme.TextStyle.BODY_STRONG, open ? Theme.GOLD_TEXT : Theme.INK_MUTED)).top();
        return row;
    }

    private static String fmt(double amount) {
        return amount == Math.rint(amount) ? String.valueOf((long) amount) : String.format(Locale.ROOT, "%.1f", amount);
    }

    private Table stepsGrid() {
        float[] widths = {26f, 150f, 206f, 0f, 236f, 62f, 196f};
        Table grid = new Table();
        grid.top().left();
        String[] heads = {"#", "Step", "Components", "Mission control", "Crew skills", "Prestige", "Goals reached"};
        for (int i = 0; i < heads.length; i++) {
            cell(grid, ui.label(heads[i].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED), widths[i])
                .padBottom(6f);
        }
        grid.row();
        grid.add(new Image(ui.solid(Theme.NAVY))).colspan(heads.length).growX().height(Theme.BORDER_EMPHASIS).row();
        int number = 0;
        for (int s = 0; s < configuration.seasons().size(); s++) {
            MissionSeason season = configuration.seasons().get(s);
            grid.add(ui.label(("Season " + (s + 1) + " of " + configuration.seasons().size()).toUpperCase(Locale.ROOT),
                Theme.TextStyle.EYEBROW, Theme.NAVY)).colspan(heads.length).left().pad(10f, 0f, 4f, 0f).row();
            for (MissionStep step : season.steps()) {
                number++;
                stepRow(grid, widths, number, step);
                grid.add(new Image(ui.solid(Theme.NAVY_BORDER))).colspan(heads.length).growX()
                    .height(Theme.BORDER_THIN).row();
            }
        }
        return grid;
    }

    private com.badlogic.gdx.scenes.scene2d.ui.Cell<Actor> cell(Table grid, Actor actor, float width) {
        com.badlogic.gdx.scenes.scene2d.ui.Cell<Actor> cell = grid.add(actor).left().padRight(14f);
        return width > 0 ? cell.width(width) : cell.expandX().fillX();
    }

    private void stepRow(Table grid, float[] widths, int number, MissionStep step) {
        cell(grid, ui.label(String.valueOf(number), Theme.TextStyle.BODY_SM, Theme.INK_MUTED), widths[0]).pad(7f, 0f,
            7f, 14f);
        cell(grid, ui.paragraph(MissionPlanScreen.stepName(configuration.id(), step), Theme.TextStyle.BODY_STRONG, Theme.INK), widths[1])
            .pad(7f, 0f, 7f, 14f);

        HorizontalGroup components = flow();
        for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
            int launch = MissionConfiguration.rocketSlotIndex(involvement.componentId());
            String label;
            String icon;
            if (launch >= 0) {
                label = (configuration.launches().size() > 1 ? "Rocket " + (launch + 1) : "Rocket");
                icon = "skill_rockets";
            } else {
                MissionComponent component = session.catalog().missionComponent(involvement.componentId()).orElseThrow();
                label = ContentNames.of(component.id());
                icon = SetSkills.icon(component.associatedSkill());
            }
            components.addActor(tag(icon, label + " " + involvement.percent() + "%", false));
        }
        cell(grid, components, widths[2]).pad(7f, 0f, 7f, 14f);

        HorizontalGroup consoles = flow();
        for (MissionStep.ControllerInvolvement involvement : step.controllerInvolvements()) {
            StaffingSlot slot = configuration.controlSlots().get(involvement.slot());
            consoles.addActor(tag(CrewSkills.icon(slot.skill()), slot.title() + " " + involvement.percent() + "%",
                slot.kind() == StaffingSlot.Kind.ASTRONAUT));
        }
        cell(grid, consoles, widths[3]).pad(7f, 0f, 7f, 14f);

        HorizontalGroup crew = flow();
        for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
            String seat = configuration.crewSlots().size() > 1 ? configuration.crewSlots().get(involvement.slot()) + " "
                : "";
            crew.addActor(tag(CrewSkills.icon(involvement.skill().name()), seat + CrewSkills.label(
                involvement.skill().name()) + " " + Math.round(involvement.percent()) + "%", false));
        }
        if (step.crewRisk()) {
            crew.addActor(ui.label("Crew at risk", Theme.TextStyle.LABEL, Theme.GOLD_TEXT));
        }
        cell(grid, crew, widths[4]).pad(7f, 0f, 7f, 14f);

        cell(grid, ui.label(step.prestigeSharePercent() + "%", Theme.TextStyle.BODY_SM, Theme.INK), widths[5])
            .pad(7f, 0f, 7f, 14f);

        Table goals = new Table();
        goals.left();
        goals.defaults().left();
        for (String goalId : step.goalIdsAchieved()) {
            goals.add(goalChip(ContentNames.of(goalId))).padBottom(2f).row();
            // What reaching it would also credit now: goals already held drop out.
            List<String> also = GoalCascade.unheld(goalId, id -> !id.equals(goalId)
                && session.state().hasAchievedOrCancelledGoal(id), session.catalog());
            if (also.size() > 1) {
                Label line = ui.paragraph("Also credits " + String.join(", ", also.subList(1, also.size()).stream()
                    .map(ContentNames::of).toList()), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
                goals.add(line).width(widths[6]).row();
            }
        }
        grid.add(goals).width(widths[6]).left().pad(7f, 0f, 7f, 0f);
        grid.row();
    }

    private HorizontalGroup flow() {
        HorizontalGroup group = new HorizontalGroup();
        group.wrap();
        group.space(4f);
        group.wrapSpace(4f);
        group.rowAlign(Align.left);
        group.align(Align.left);
        return group;
    }

    private Table tag(String icon, String text, boolean navy) {
        Table tag = new Table();
        tag.setBackground(ui.rect(Theme.SURFACE_CARD, navy ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        tag.pad(2f, 6f, 2f, 6f);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(navy ? Theme.NAVY : Theme.INK_MUTED);
        tag.add(glyph).size(11f).padRight(4f);
        tag.add(ui.label(text, Theme.TextStyle.LABEL, navy ? Theme.NAVY : Theme.INK));
        return tag;
    }

    private Table goalChip(String text) {
        Table chip = new Table();
        chip.setBackground(ui.rect(new com.badlogic.gdx.graphics.Color(Theme.SURFACE_CARD).lerp(Theme.SUCCESS, 0.08f),
            Theme.SUCCESS, Theme.BORDER_THIN));
        chip.pad(2f, 6f, 2f, 6f);
        chip.add(ui.label(text, Theme.TextStyle.LABEL, Theme.SUCCESS));
        return chip;
    }
}
