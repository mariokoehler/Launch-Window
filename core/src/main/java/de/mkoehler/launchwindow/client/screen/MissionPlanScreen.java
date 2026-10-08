package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.OutcomeBar;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Staffing;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.MissionDraft;
import de.mkoehler.launchwindow.client.view.Missions;
import de.mkoehler.launchwindow.client.view.PayloadRnd;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.GoalCascade;
import de.mkoehler.launchwindow.rules.MissionAssessment;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionLaunchOutcome;
import de.mkoehler.launchwindow.rules.MissionProgress;
import de.mkoehler.launchwindow.rules.MissionStepResolution;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The planning hub for one configuration ({@code ui-spec.md} &sect;4.6.7): the mission assessment
 * (the odds, computed in the client by {@link MissionAssessment}), rocket assignment, and the
 * Payload R&amp;D board filtered to the mission's payloads, with the way to assembly. Nothing is
 * sent from here; the rocket choice and staffing live in the session's {@link MissionDraft}.
 *
 * @author Mario Koehler
 */
public final class MissionPlanScreen extends ChromeScreen {

    /** A step is weak when it ends the mission at least this often (unconfirmed, from the mockup). */
    static final double WEAK_STEP = 0.10;

    /**
     * The hub's three parts.
     */
    public enum Tab {
        /** The odds. */
        ASSESSMENT("Mission assessment"),
        /** A rocket per launch. */
        ROCKETS("Rocket assignment"),
        /** R&amp;D on the mission's payloads. */
        RND("Payload R&D");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private final MissionConfiguration configuration;
    private final Program program;
    private Tab tab;

    /**
     * Builds the screen from the session.
     *
     * @param game            the game, in a session
     * @param configurationId the configuration
     * @param tab             the part to show
     */
    public MissionPlanScreen(LaunchWindowGame game, String configurationId, Tab tab) {
        this(game, game.session().catalog().missionConfiguration(configurationId).orElseThrow(), tab);
    }

    private MissionPlanScreen(LaunchWindowGame game, MissionConfiguration configuration, Tab tab) {
        super(game, MissionConfigsScreen.crumbs(game.session(), configuration.programId(), configuration, "Plan"),
            "Headquarters · " + ContentNames.of(configuration.programId()) + " · " + Missions.name(configuration),
            "Plan the mission");
        this.configuration = configuration;
        this.program = session.catalog().program(configuration.programId()).orElseThrow();
        this.tab = tab;
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(session.planningFromList() ? new PlanMissionScreen(game)
            : new MissionStepsScreen(game, configuration.id()));
    }

    private boolean readOnly() {
        return !session.state().getOpenedProgramIds().contains(program.id());
    }

    /**
     * Shows one of the three parts.
     *
     * @param tab the part
     */
    public void select(Tab tab) {
        this.tab = tab;
        rebuild();
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.MISSION_PLAN;
    }

    @Override
    protected void rebuild() {
        MissionDraft draft = session.draft(configuration);
        content.clearChildren();
        content.top();
        content.defaults().growX();

        Table bar = new Table();
        for (Tab each : Tab.values()) {
            bar.add(tabButton(each)).padRight(Theme.SPACE_2);
        }
        bar.add().expandX();
        bar.add(ui.label(readOnly() ? "Open the program to assemble the mission."
            : "Assembly picks who flies and takes the payment.", Theme.TextStyle.CAPTION, Theme.ON_PAGE_MUTED))
            .padRight(Theme.SPACE_3);
        TextButton assemble = actionButton("Assemble mission", Theme.ButtonKind.PRIMARY,
            () -> game.setScreen(new AssembleMissionScreen(game, configuration.id())));
        assemble.setDisabled(readOnly());
        bar.add(assemble);
        content.add(bar).padBottom(Theme.SPACE_4).row();
        if (readOnly()) {
            content.add(MissionConfigsScreen.readOnlyNotice(this, program, "Planning is read-only: you can study the "
                + "odds, but not assign R&D or assemble the mission.")).padBottom(Theme.SPACE_4).row();
        }
        Table body = switch (tab) {
            case ASSESSMENT -> assessment(draft);
            case ROCKETS -> rockets(draft);
            case RND -> rnd();
        };
        content.add(body).expand().fill();
    }

    private Table tabButton(Tab each) {
        boolean active = each == tab;
        Table button = new Table();
        button.setBackground(ui.navTab(active));
        button.pad(Theme.SPACE_2, Theme.SPACE_4, Theme.SPACE_2, Theme.SPACE_4);
        button.add(ui.label(each.label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
            active ? Theme.NAVY : Theme.ON_PAGE_STRONG));
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(() -> select(each)));
        return button;
    }

    // ---- Mission assessment

    private Table assessment(MissionDraft draft) {
        PlayerSnapshot state = session.state();
        MissionAssessment.Assessment odds = draft.assess(state, session.catalog());
        Table body = new Table();
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        left.add(headline(odds, draft)).row();
        left.add(componentsCard(draft)).row();
        left.add(staffingCard()).row();
        for (Table notice : weakSpots(draft, odds)) {
            left.add(notice).row();
        }
        body.add(scrollPane(left)).width(520f).top().fillY().padRight(Theme.SPACE_5);
        body.add(scrollPane(oddsTable(odds))).expand().fill();
        return body;
    }

    private Table headline(MissionAssessment.Assessment odds, MissionDraft draft) {
        Table card = ui.card();
        card.defaults().left();
        card.add(ui.label("IF IT LAUNCHES AS PLANNED", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).colspan(3)
            .padBottom(Theme.SPACE_3).row();
        card.add(bigStat(pct(odds.completion()), "Completes", Theme.INK)).padRight(Theme.SPACE_6);
        if (Missions.crewCount(configuration) > 0) {
            card.add(bigStat(pct(odds.crewLoss()), "Crew lost", Theme.GOLD_TEXT)).padRight(Theme.SPACE_6);
        }
        card.add(bigStat(String.format(Locale.ROOT, "%,d", Missions.nextPrestige(configuration, session.state())),
            "Prestige", Theme.INK)).expandX().left().row();
        String crew = draft.crewSlotIds().stream().filter(java.util.Objects::nonNull)
            .map(id -> session.state().getAstronauts().stream().filter(a -> a.getId().equals(id)).findFirst()
                .map(a -> a.getName()).orElse(id)).reduce((a, b) -> a + ", " + b).orElse(null);
        String rockets = draft.rocketIds().stream().filter(id -> id != null && !id.isEmpty()).map(ContentNames::of)
            .reduce((a, b) -> a + ", " + b).orElse("no rocket");
        card.add(ui.paragraph("With your plan: " + (crew == null ? "" : crew + " aboard, ") + rockets
            + ", and the staff Auto-assign picked unless you changed it in assembly. Expert rescues, changes "
            + "between seasons and random events aren't counted.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .colspan(3).growX().padTop(Theme.SPACE_3);
        return card;
    }

    /**
     * The people this mission needs against those free, who's coming, and the skill each seat reads
     * (play-test request 2026-10-07: plan hiring and training before assembly).
     */
    private Table staffingCard() {
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label("STAFFING", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        Table grid = new Table();
        grid.defaults().right().padBottom(4f);
        grid.add().expandX();
        for (String head : new String[] {"Needed", "Free", "Training", "Basic", "Busy"}) {
            grid.add(ui.label(head.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(62f);
        }
        grid.row();
        List<Staffing.Group> groups = Staffing.of(configuration, session.state());
        for (Staffing.Group group : groups) {
            grid.add(ui.label(Character.toUpperCase(group.group().charAt(0)) + group.group().substring(1),
                Theme.TextStyle.BODY_SM, Theme.INK)).left();
            grid.add(ui.label(String.valueOf(group.needed()), Theme.TextStyle.BODY_STRONG, Theme.INK)).width(62f);
            grid.add(ui.label(String.valueOf(group.free()), Theme.TextStyle.BODY_STRONG,
                group.isShort() ? Theme.DANGER : Theme.SUCCESS)).width(62f);
            grid.add(ui.label(String.valueOf(group.inTraining()), Theme.TextStyle.BODY_SM, Theme.INK)).width(62f);
            grid.add(ui.label(String.valueOf(group.inBasic()), Theme.TextStyle.BODY_SM, Theme.INK)).width(62f);
            grid.add(ui.label(String.valueOf(group.busy()), Theme.TextStyle.BODY_SM, Theme.INK)).width(62f).row();
        }
        card.add(grid).padBottom(Theme.SPACE_2).row();
        for (Staffing.Group group : groups) {
            String advice = Staffing.advice(group);
            if (advice != null) {
                card.add(ui.paragraph(advice, Theme.TextStyle.CAPTION, Theme.DANGER)).padBottom(Theme.SPACE_1).row();
            }
        }
        card.add(ui.label("SEATS AND THE SKILL EACH READS", Theme.TextStyle.LABEL, Theme.INK_MUTED))
            .padTop(Theme.SPACE_2).padBottom(4f).row();
        Table seats = new Table();
        seats.defaults().left().top().padBottom(2f);
        for (String[] seat : Staffing.seats(configuration)) {
            seats.add(ui.label(seat[0], Theme.TextStyle.BODY_SM, Theme.INK)).width(230f).padRight(Theme.SPACE_2);
            seats.add(ui.label(seat[1], Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).expandX().row();
        }
        card.add(seats).row();
        return card;
    }

    private Table bigStat(String value, String label, Color color) {
        Table stat = new Table();
        stat.defaults().left();
        stat.add(ui.label(value, Theme.TextStyle.HEADING_LG, color)).row();
        stat.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED));
        return stat;
    }

    /**
     * Returns the components the mission flies, in step order, with the chosen rockets in their
     * launch slots.
     */
    private List<String> flownComponents(MissionDraft draft) {
        Set<String> ids = new LinkedHashSet<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    String id = MissionLaunch.resolveComponentId(involvement.componentId(), draft.rocketIds());
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private Table componentsCard(MissionDraft draft) {
        PlayerSnapshot state = session.state();
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label("COMPONENTS AS THEY'LL FLY", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_3).row();
        for (String id : flownComponents(draft)) {
            MissionComponent component = session.catalog().missionComponent(id).orElseThrow();
            OwnedMissionComponent owned = state.getMissionComponents().get(id);
            double current = owned == null ? 0 : owned.getCurrentReliability();
            double modifier = MissionProgress.goalModifierFor(state::hasAchievedOrCancelledGoal, configuration, id);
            double flies = MissionStepResolution.effectiveReliability(current, modifier);
            Table head = new Table();
            Image glyph = new Image(ui.icon(SetSkills.icon(component.associatedSkill())), Scaling.fit);
            glyph.setColor(Theme.NAVY);
            head.add(glyph).size(14f).padRight(6f);
            head.add(ui.label(ContentNames.of(id), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX().left();
            head.add(ui.label(String.format(Locale.ROOT, "%.1f%%", flies), Theme.TextStyle.BODY_STRONG, Theme.INK));
            card.add(head).padBottom(4f).row();
            card.add(new ReliabilityBar(ui, current, component.maxRdReliability(), component.maxReliability(),
                owned == null ? 0 : owned.getFloorReliability())).height(14f).padBottom(4f).row();
            for (MissionConfiguration.GoalModifier dependency : configuration.goalDependencies()) {
                if (dependency.componentId().equals(id) && !state.hasAchievedOrCancelledGoal(dependency.goalId())) {
                    card.add(priceRow(ContentNames.of(dependency.goalId()) + " not reached",
                        "−" + dependency.amount(), Theme.GOLD_TEXT)).row();
                }
            }
            for (MissionConfiguration.GoalModifier boost : configuration.goalBoosts()) {
                if (boost.componentId().equals(id) && state.hasAchievedOrCancelledGoal(boost.goalId())) {
                    card.add(priceRow(ContentNames.of(boost.goalId()) + " reached", "+" + boost.amount(),
                        Theme.SUCCESS)).row();
                }
            }
            card.add().height(Theme.SPACE_3).row();
        }
        return card;
    }

    private Table priceRow(String label, String value, Color valueColor) {
        Table row = new Table();
        row.add(ui.label(label, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        row.add(ui.label(value, Theme.TextStyle.CAPTION, valueColor));
        return row;
    }

    private List<Table> weakSpots(MissionDraft draft, MissionAssessment.Assessment odds) {
        PlayerSnapshot state = session.state();
        List<Table> notices = new ArrayList<>();
        for (int launch = 0; launch < draft.rocketIds().size(); launch++) {
            if (draft.rocketIds().get(launch) == null) {
                notices.add(ui.notice(Theme.NoticeKind.DANGER, "No rocket for launch " + (launch + 1) + ".",
                    "None of your open rockets can fly it. Rocket programs open in the VAB."));
            }
        }
        for (MissionConfiguration.GoalModifier dependency : configuration.goalDependencies()) {
            if (state.hasAchievedOrCancelledGoal(dependency.goalId())) {
                continue;
            }
            String lifter = lifter(dependency.goalId());
            notices.add(ui.notice(Theme.NoticeKind.WARNING, ContentNames.of(dependency.goalId()) + " not reached.",
                ContentNames.of(dependency.componentId()) + " flies " + dependency.amount()
                    + " points lower until you reach it." + (lifter == null ? "" : " " + lifter)));
        }
        List<String> weak = new ArrayList<>();
        double worst = 0;
        int number = 0;
        for (MissionAssessment.StepOdds step : odds.steps()) {
            number++;
            if (step.endsFlight() >= WEAK_STEP) {
                weak.add(String.valueOf(number));
                worst = Math.max(worst, step.endsFlight());
            }
        }
        if (!weak.isEmpty()) {
            notices.add(ui.notice(Theme.NoticeKind.INFO, (weak.size() == 1 ? "Step " + weak.get(0) + " is weak."
                : "Steps " + String.join(", ", weak) + " are weak."), "Each ends the mission at least "
                + pct(WEAK_STEP) + " of the time, up to " + pct(worst) + ". More reliable components or better staff "
                + "for them lower that."));
        }
        return notices;
    }

    /**
     * Names a configuration of the player's open programs that reaches a goal, directly or through
     * the cascade, other than this one.
     */
    private String lifter(String goalId) {
        for (MissionConfiguration other : session.catalog().missionConfigurations()) {
            if (other.id().equals(configuration.id())
                || !session.state().getOpenedProgramIds().contains(other.programId())) {
                continue;
            }
            for (MissionSeason season : other.seasons()) {
                for (MissionStep step : season.steps()) {
                    for (String reached : step.goalIdsAchieved()) {
                        if (GoalCascade.unheld(reached, id -> false, session.catalog()).contains(goalId)) {
                            return "Flying " + ContentNames.of(other.programId()) + " " + Missions.name(other)
                                .toLowerCase(Locale.ROOT) + " reaches it.";
                        }
                    }
                }
            }
        }
        return null;
    }

    private Table oddsTable(MissionAssessment.Assessment odds) {
        Table card = ui.card();
        card.top();
        Table head = new Table();
        head.add(ui.label("Odds per step", Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        String[] names = {"Success", "Glitch, solved", "Minor glitch", "Failure (ends the mission)", "Catastrophic"};
        for (int i = 0; i < names.length; i++) {
            head.add(new Image(ui.solid(OutcomeBar.COLORS[i]))).size(10f).padRight(5f);
            head.add(ui.label(names[i], Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(14f);
        }
        card.add(head).growX().padBottom(Theme.SPACE_3).row();

        float[] widths = {34f, 184f, 100f, 100f, 100f, 100f, 96f, 0f};
        String[] heads = {"#", "Step", "Hardware", "Success", "Saved", "Ends it", "Crew lost", "Outcomes"};
        Table grid = new Table();
        grid.top().left();
        for (int i = 0; i < heads.length; i++) {
            Label label = ui.label(heads[i].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
            label.setAlignment(i >= 2 && i <= 6 ? Align.right : Align.left);
            column(grid.add(label), widths[i]).pad(0f, 7f, 6f, 7f);
        }
        grid.row();
        grid.add(new Image(ui.solid(Theme.NAVY))).colspan(heads.length).growX().height(Theme.BORDER_EMPHASIS).row();
        int number = 0;
        for (MissionAssessment.StepOdds step : odds.steps()) {
            number++;
            Table row = new Table();
            if (step.endsFlight() >= WEAK_STEP) {
                row.setBackground(ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.09f)));
            }
            Table nameCell = new Table();
            nameCell.left();
            nameCell.add(ui.label(stepName(configuration.id(), step.step()), Theme.TextStyle.BODY_STRONG, Theme.INK)).left();
            if (step.endsFlight() >= WEAK_STEP) {
                Table weak = new Table();
                weak.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.GOLD_TEXT, Theme.BORDER_THIN));
                weak.pad(1f, 5f, 1f, 5f);
                weak.add(ui.label("WEAK", Theme.TextStyle.LABEL, Theme.GOLD_TEXT));
                nameCell.add(weak).padLeft(6f);
            }
            Object[] cells = {ui.label(String.valueOf(number), Theme.TextStyle.BODY_SM, Theme.INK_MUTED), nameCell,
                num(String.format(Locale.ROOT, "%.1f", step.hardware())), num(pct(step.success())),
                num(pct(step.solved())), num(pct(step.endsFlight()), true), num(step.step().crewRisk()
                    ? pct(step.catastrophic()) : "—"),
                new OutcomeBar(ui, step.success(), step.solved(), step.glitch(), step.failure(), step.catastrophic())};
            for (int i = 0; i < cells.length; i++) {
                // The outcome bar keeps its own 12 px height; the row is 46 px tall.
                column(row.add((com.badlogic.gdx.scenes.scene2d.Actor) cells[i]), widths[i])
                    .height(i == cells.length - 1 ? 12f : 46f).pad(0f, 7f, 0f, 7f);
            }
            grid.add(row).colspan(heads.length).growX().row();
            grid.add(new Image(ui.solid(Theme.NAVY_BORDER))).colspan(heads.length).growX()
                .height(Theme.BORDER_THIN).row();
        }
        card.add(grid).growX().row();
        card.add(ui.paragraph("Hardware is the components' weighted reliability after goal penalties. Success is the "
            + "first roll, with the crew's skills blended in where the hardware is above "
            + Math.round(MissionStepResolution.CREW_BLEND_HARDWARE_THRESHOLD) + ". If it fails, the controllers get "
            + "a saving roll. A minor glitch lets the mission go on at half the step's prestige.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX().padTop(Theme.SPACE_3);
        return card;
    }

    private static <T extends com.badlogic.gdx.scenes.scene2d.Actor> com.badlogic.gdx.scenes.scene2d.ui.Cell<T>
            column(com.badlogic.gdx.scenes.scene2d.ui.Cell<T> cell, float width) {
        return width > 0 ? cell.width(width) : cell.expandX().fillX();
    }

    private Label num(String text) {
        return num(text, false);
    }

    private Label num(String text, boolean strong) {
        Label label = ui.label(text, strong ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY_SM, Theme.INK);
        label.setAlignment(Align.right);
        return label;
    }

    /**
     * Names a step from the pack's text ("Launch preparations" for {@code 01-launch-preparations}).
     *
     * @param configurationId the configuration, for steps it names on its own
     * @param step            the step
     * @return the name
     */
    static String stepName(String configurationId, MissionStep step) {
        return ContentNames.step(configurationId, step.id());
    }

    /**
     * Words a chance as a percentage, with a decimal under 10 %.
     *
     * @param p the chance, 0-1
     * @return the words
     */
    static String pct(double p) {
        double percent = p * 100;
        return percent > 0 && percent < 10 ? String.format(Locale.ROOT, "%.1f%%", percent)
            : String.format(Locale.ROOT, "%d%%", Math.round(percent));
    }

    // ---- Rocket assignment

    private Table rockets(MissionDraft draft) {
        Table body = new Table();
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        for (int launch = 0; launch < configuration.launches().size(); launch++) {
            left.add(launchCard(draft, launch)).row();
        }
        body.add(scrollPane(left)).expand().fill().padRight(Theme.SPACE_5);

        Table right = new Table();
        right.top();
        right.defaults().growX().padBottom(Theme.SPACE_4);
        right.add(ui.notice(Theme.NoticeKind.INFO, "Nothing is submitted here.", "Your pick carries into Assemble "
            + "mission, where you pay for it with the rest of the mission.")).row();
        Table about = ui.card();
        about.defaults().growX().left().padBottom(Theme.SPACE_2);
        about.add(ui.label("WHICH ROCKETS ARE LISTED", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        about.add(ui.paragraph("Rockets you own whose program is open. Each launch gets its own picker: a joint flight "
            + "has two.", Theme.TextStyle.BODY_SM, Theme.INK)).row();
        about.add(ui.paragraph("Spare capacity is what's left after the payload. Greyed rockets say why they can't fly "
            + "this launch.", Theme.TextStyle.BODY_SM, Theme.INK)).row();
        about.add(actionButton("Rocket programs in the VAB", Theme.ButtonKind.SECONDARY, this::goVab)).left()
            .fill(false, false);
        right.add(about);
        body.add(right).width(440f).top();
        return body;
    }

    private Table launchCard(MissionDraft draft, int launch) {
        MissionConfiguration.Launch spec = configuration.launches().get(launch);
        Table card = ui.card();
        card.defaults().growX().left();
        card.add(ui.label(("Launch " + (launch + 1) + " of " + configuration.launches().size()).toUpperCase(
            Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        card.add(ui.label(Missions.launchLabel(spec.launchType()), Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(4f).row();
        card.add(ui.paragraph("Payload: " + Missions.payload(spec, session.catalog()) + "."
            + (spec.launchType().humanRated() && spec.launchType().needsRocket()
            ? " A crewed launch needs a human-rated rocket." : ""),
            Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        if (!spec.launchType().needsRocket()) {
            card.add(ui.paragraph("A carrier aircraft drops the craft: there's no rocket to pick or pay for.",
                Theme.TextStyle.BODY, Theme.INK_MUTED));
            return card;
        }
        List<Missions.RocketChoice> choices = Missions.rocketChoices(spec, session.state(), session.catalog());
        if (choices.isEmpty()) {
            card.add(ui.paragraph("No rocket programs open. Open one in the VAB.", Theme.TextStyle.BODY,
                Theme.INK_MUTED));
        }
        for (Missions.RocketChoice choice : choices) {
            card.add(rocketRow(draft, launch, spec, choice)).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table rocketRow(MissionDraft draft, int launch, MissionConfiguration.Launch spec,
                            Missions.RocketChoice choice) {
        boolean selected = choice.rocket().id().equals(draft.rocketIds().get(launch));
        Table row = new Table();
        row.setBackground(ui.rect(Theme.SURFACE_CARD, selected ? Theme.NAVY : Theme.NAVY_BORDER,
            selected ? Theme.BORDER_EMPHASIS : Theme.BORDER_THIN));
        row.pad(14f, 16f, 14f, 16f);
        Table radio = new Table();
        radio.setBackground(ui.rect(Theme.SURFACE_CARD, selected ? Theme.NAVY : Theme.INK_MUTED,
            Theme.BORDER_EMPHASIS));
        if (selected) {
            radio.add(new Image(ui.solid(Theme.NAVY))).size(8f);
        }
        row.add(radio).size(16f).padRight(16f);
        Image glyph = new Image(ui.icon(SetSkills.icon(choice.rocket().associatedSkill())), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        row.add(glyph).size(30f).padRight(16f);

        Table about = new Table();
        about.left();
        about.defaults().left();
        Table name = new Table();
        name.add(ui.label(ContentNames.of(choice.rocket().id()), Theme.TextStyle.BODY_STRONG, Theme.INK))
            .padRight(Theme.SPACE_2);
        boolean humanRated = choice.rocket().associatedSkill() == de.mkoehler.launchwindow.model.SetSkill
            .HUMAN_RATED_ROCKETS;
        Table tag = new Table();
        tag.setBackground(ui.rect(Theme.SURFACE_CARD, humanRated ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        tag.pad(2f, 6f, 2f, 6f);
        tag.add(ui.label(humanRated ? "Human-rated" : "Not human-rated", Theme.TextStyle.LABEL,
            humanRated ? Theme.NAVY : Theme.INK_MUTED));
        name.add(tag);
        about.add(name).padBottom(2f).row();
        String place = Missions.launchLabel(spec.launchType()).replace(", human-rated", "").toLowerCase(Locale.ROOT);
        about.add(ui.label(choice.lift() > 0 ? String.format(Locale.ROOT, "Lifts %,.0f kg to %s · %,.0f kg spare",
            choice.lift(), place, choice.spare()) : "Can't fly to " + place, Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
            .row();
        if (!choice.reasons().isEmpty()) {
            about.add(ui.label(String.join(" ", choice.reasons()), Theme.TextStyle.BODY_SM, Theme.DANGER)).row();
        }
        row.add(about).expandX().fillX().padRight(16f);
        Table reliability = new Table();
        reliability.add(new ReliabilityBar(ui, choice.owned().getCurrentReliability(), choice.rocket()
            .maxRdReliability(), choice.rocket().maxReliability(), choice.owned().getFloorReliability()))
            .growX().height(14f).row();
        reliability.add(ui.label(String.format(Locale.ROOT, "%.1f%%", choice.owned().getCurrentReliability()),
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left();
        row.add(reliability).width(260f).padRight(16f);
        Table cost = new Table();
        cost.defaults().right();
        cost.add(ui.label(money(choice.rocket().unitCost()), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        cost.add(ui.label("per launch", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(cost).width(130f).right();
        if (!choice.eligible()) {
            row.setColor(1f, 1f, 1f, 0.55f);
        }
        row.setTouchable(Touchable.enabled);
        row.addListener(clicked(() -> {
            if (!choice.eligible()) {
                toast(Refusals.text(MissionLaunchOutcome.ROCKET_CANNOT_LIFT_PAYLOAD.name()), Theme.NoticeKind.INFO);
                return;
            }
            draft.setRocket(launch, choice.rocket().id());
            rebuild();
        }));
        return row;
    }

    // ---- Payload R&D

    private Table rnd() {
        Set<String> payloads = new LinkedHashSet<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                    if (MissionConfiguration.rocketSlotIndex(involvement.componentId()) < 0) {
                        payloads.add(involvement.componentId());
                    }
                }
            }
        }
        RndCards cards = new RndCards(this, "Payload R&D", !readOnly());
        Table body = new Table();
        Table left = new Table();
        left.top();
        left.defaults().growX().padBottom(Theme.SPACE_4);
        List<PayloadRnd.Card> shown = PayloadRnd.cards(session.state(), session.catalog()).stream()
            .filter(card -> payloads.contains(card.component().id())).toList();
        if (shown.isEmpty()) {
            Table empty = ui.card();
            empty.add(ui.paragraph("None of this mission's payloads is owned yet. Opening the program creates them.",
                Theme.TextStyle.BODY, Theme.INK_MUTED)).growX();
            left.add(empty).row();
        }
        for (PayloadRnd.Card card : shown) {
            left.add(cards.card(card)).row();
        }
        body.add(scrollPane(left)).expand().fill().padRight(Theme.SPACE_5);
        Table about = ui.card();
        about.top();
        about.defaults().growX().left().padBottom(Theme.SPACE_2);
        about.add(ui.label("WHY IT MATTERS HERE", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        about.add(ui.paragraph("The SET Centre's payload R&D board, filtered to this mission's payloads. Slot 1 earns "
            + "the most each season, slot 4 the least. R&D stops at the gold R&D cap; only flying takes a component "
            + "beyond it.", Theme.TextStyle.BODY_SM, Theme.INK)).row();
        about.add(ui.paragraph("Researchers keep working while you plan. R&D gains land at the next turn.",
            Theme.TextStyle.BODY_SM, Theme.INK));
        body.add(about).width(440f).top();
        return body;
    }
}
