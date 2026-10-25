package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.HandsOnAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.DecisionView;
import de.mkoehler.launchwindow.client.view.HandsOnView;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.HandsOnLevel;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.HandsOn;
import de.mkoehler.launchwindow.rules.RdDecisions;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The hands-on decision's screen ({@code ui-spec.md} component 26, type 11, the canvas
 * {@code Launch Window — Hands-on}): the objective bar, the command table, the queue panel and, at
 * the end, the result panel. Every step goes to the server, which rolls and answers with the new
 * state; this screen only draws it.
 *
 * @author Mario Koehler
 */
public final class HandsOnScreen extends ChromeScreen {

    /** The queue panel's width. */
    private static final float QUEUE_WIDTH = 600f;

    private final String componentId;
    private final boolean rocket;
    private final Table roundChip = new Table();
    /** The round whose results the player is still looking at, or 0. */
    private int showingRound;

    /**
     * Opens the screen on the hands-on decision in progress.
     *
     * @param game the game, in a session
     */
    public HandsOnScreen(LaunchWindowGame game) {
        super(game, crumbs(game), "Hands-on", "Hands-on");
        PendingDecision pending = session.state().getPendingDecision();
        this.componentId = pending.componentId();
        this.rocket = DecisionView.rocket(componentId, session.catalog());
        Decisions.Decision decision = HandsOnView.decision(session.state()).orElseThrow();
        setTitles("Hands-on · " + ContentNames.of(componentId), decision.headline());
        titleActions.add(roundChip);
        rebuild();
    }

    private static List<Crumb> crumbs(LaunchWindowGame game) {
        PendingDecision pending = game.session().state().getPendingDecision();
        boolean rocket = pending != null && DecisionView.rocket(pending.componentId(), game.session().catalog());
        Crumb board = new Crumb(rocket ? "Rocket R&D" : "Payload R&D", screen -> screen.game.setScreen(
            new RndBoardScreen(screen.game, rocket ? RndBoardScreen.Board.ROCKET : RndBoardScreen.Board.PAYLOAD)));
        return List.of(Crumb.SPACE_COMPLEX, board, new Crumb("Hands-on", null));
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.RND;
    }

    @Override
    protected void goBack() {
        game.setScreen(new RndBoardScreen(game, rocket ? RndBoardScreen.Board.ROCKET : RndBoardScreen.Board.PAYLOAD));
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        content.defaults().growX();
        PendingDecision pending = session.state().getPendingDecision();
        Decisions.Decision decision = HandsOnView.decision(session.state()).orElse(null);
        if (pending == null || decision == null || pending.handsOn() == null
                || !pending.componentId().equals(componentId)) {
            content.add(ui.notice(Theme.NoticeKind.INFO, "No campaign here.", "The turn resolved and settled it. "
                + "Its outcome is in the turn report.")).row();
            return;
        }
        HandsOnLevel level = decision.handsOn();
        HandsOnState state = pending.handsOn();
        boolean finished = state.phase() == HandsOnState.Phase.FINISHED;
        roundChip.clearChildren();
        roundChip.setBackground(ui.solid(Theme.SURFACE_CARD));
        roundChip.pad(9f, 16f, 9f, 16f);
        roundChip.add(ui.label(finished ? "CAMPAIGN OVER" : ("Round " + state.round() + " of " + level.rounds())
            .toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.NAVY));

        content.add(objectiveBar(level, decision, state)).padBottom(Theme.SPACE_4).row();
        Table grid = new Table();
        grid.top();
        grid.add(commandTable(level, state)).width(0f).expandX().fillX().fillY().top().padRight(Theme.SPACE_4);
        grid.add(finished ? resultPanel(level, decision, state) : queuePanel(level, state)).width(QUEUE_WIDTH)
            .fillY().top();
        content.add(scrollPane(grid)).expand().fill();
    }

    // ---- The objective bar

    private Table objectiveBar(HandsOnLevel level, Decisions.Decision decision, HandsOnState state) {
        Table bar = ui.card();
        bar.pad(14f, 18f, 14f, 18f);
        Map<String, Integer> coming = HandsOn.queuedOutput(level, state);
        Table objective = new Table();
        objective.left();
        objective.add(ui.label(("Objective · " + HandsOnView.worth(decision, HandsOnState.Result.SUCCESS))
            .toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).left().padBottom(6f).row();
        Table chips = new Table();
        level.objective().forEach((resource, target) -> {
            boolean met = state.held(resource) >= target;
            Table chip = new Table();
            chip.setBackground(met ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.SUCCESS, 0.07f), Theme.SUCCESS,
                Theme.BORDER_EMPHASIS) : ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_EMPHASIS));
            chip.pad(8f, 12f, 8f, 12f);
            chip.add(checkbox(met, false)).padRight(10f);
            chip.add(tile(level, resource, 32f)).padRight(10f);
            Table words = new Table();
            words.defaults().left();
            words.add(ui.label(state.held(resource) + "  ›  " + target, Theme.TextStyle.HEADING_SM, Theme.INK))
                .row();
            words.add(ui.label(level.resource(resource).orElseThrow().name(), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).row();
            int queued = coming.getOrDefault(resource, 0);
            if (queued > 0) {
                words.add(ui.label("+" + queued + " on execution", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            }
            chip.add(words);
            chips.add(chip).padRight(10f);
        });
        objective.add(chips).left();
        bar.add(objective).left().padRight(28f);

        boolean bonusMet = HandsOn.meets(state.held(), level.bonus());
        Table bonus = new Table();
        bonus.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.08f), Theme.GOLD,
            Theme.BORDER_EMPHASIS));
        bonus.pad(8f, 12f, 8f, 12f);
        bonus.add(checkbox(bonusMet, true)).padRight(12f);
        bonus.add(ui.label("BONUS", Theme.TextStyle.EYEBROW, Theme.GOLD_TEXT)).padRight(12f);
        Table words = new Table();
        words.defaults().left();
        words.add(ui.label(HandsOnView.target(level, level.bonus()), Theme.TextStyle.BODY_SM, Theme.INK)).row();
        words.add(ui.label(HandsOnView.worth(decision, HandsOnState.Result.BONUS) + ", and the campaign ends there",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        bonus.add(words);
        bar.add(bonus).expandX().left().bottom();

        Table counters = new Table();
        Map<String, Integer> paid = paid(level, state);
        for (HandsOnLevel.Resource resource : level.resources()) {
            if (level.objective().containsKey(resource.id())) {
                continue;
            }
            Table counter = new Table();
            counter.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            counter.pad(6f, 12f, 6f, 12f);
            counter.defaults().left();
            counter.add(ui.label(resource.name().toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
                .row();
            Table value = new Table();
            value.add(tile(level, resource.id(), 24f)).padRight(6f);
            value.add(ui.label(String.valueOf(state.held(resource.id())), Theme.TextStyle.HEADING_SM, Theme.INK));
            counter.add(value).row();
            int spent = paid.getOrDefault(resource.id(), 0);
            counter.add(ui.label(spent > 0 ? (state.held(resource.id()) + spent) + " at the round's start · " + spent
                + " paid" : " ", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            counters.add(counter).minWidth(104f).padLeft(10f);
        }
        bar.add(counters).right().bottom();
        return bar;
    }

    /** What the queued commands have paid, by resource. */
    private static Map<String, Integer> paid(HandsOnLevel level, HandsOnState state) {
        Map<String, Integer> paid = new java.util.LinkedHashMap<>();
        for (String id : state.queue()) {
            level.command(id).orElseThrow().input().forEach((resource, amount) -> paid.merge(resource, amount,
                Integer::sum));
        }
        return paid;
    }

    private Table checkbox(boolean on, boolean bonus) {
        Table box = new Table();
        box.setBackground(on ? ui.solid(Theme.SUCCESS) : ui.rect(Theme.SURFACE_CARD, bonus ? Theme.GOLD_TEXT
            : Theme.NAVY, Theme.BORDER_EMPHASIS));
        if (on) {
            Image check = new Image(ui.icon("check"), Scaling.fit);
            check.setColor(Theme.SURFACE_CARD);
            box.add(check).size(12f);
        }
        Table wrap = new Table();
        wrap.add(box).size(18f);
        return wrap;
    }

    // ---- Resources

    private static Color fill(HandsOnLevel level, String resource) {
        return switch (level.resource(resource).orElseThrow().color()) {
            case "gold" -> Theme.GOLD;
            case "danger" -> Theme.DANGER;
            case "success" -> Theme.SUCCESS;
            default -> Theme.NAVY;
        };
    }

    /** A resource's colour as small text or an icon on white: gold reads as gold-text there. */
    private static Color ink(HandsOnLevel level, String resource) {
        Color fill = fill(level, resource);
        return fill == Theme.GOLD ? Theme.GOLD_TEXT : fill;
    }

    /** A resource's icon on its colour. */
    private Table tile(HandsOnLevel level, String resource, float size) {
        Table tile = new Table();
        tile.setBackground(ui.solid(fill(level, resource)));
        Image icon = new Image(ui.icon(level.resource(resource).orElseThrow().icon()), Scaling.fit);
        icon.setColor(fill(level, resource) == Theme.GOLD ? Theme.NAVY : Theme.SURFACE_CARD);
        tile.add(icon).size(size * 0.62f);
        Table wrap = new Table();
        wrap.add(tile).size(size);
        return wrap;
    }

    /** Amounts as icons in their colours, a number before the icon when above 1. */
    private Table amounts(HandsOnLevel level, Map<String, Integer> amounts) {
        Table row = new Table();
        row.left();
        if (amounts.isEmpty()) {
            row.add(ui.label("nothing", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            return row;
        }
        amounts.forEach((resource, amount) -> {
            if (amount > 1) {
                row.add(ui.label(String.valueOf(amount), Theme.TextStyle.BODY_STRONG, ink(level, resource)))
                    .padRight(2f);
            }
            Image icon = new Image(ui.icon(level.resource(resource).orElseThrow().icon()), Scaling.fit);
            icon.setColor(ink(level, resource));
            row.add(icon).size(16f).padRight(10f);
            ui.tooltip(icon, HandsOnView.amount(level, resource, amount), null);
        });
        return row;
    }

    // ---- The command table

    private Table commandTable(HandsOnLevel level, HandsOnState state) {
        Table card = ui.card();
        card.pad(0f, 0f, 0f, 0f);
        card.top();
        card.defaults().growX();
        boolean busy = state.phase() != HandsOnState.Phase.PLANNING || showingRound > 0;
        Table head = new Table();
        head.pad(10f, 0f, 8f, 16f);
        head.add().width(84f);
        head.add(ui.label("COMMAND", Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(0f).expandX().fillX().left()
            .padLeft(12f);
        head.add(ui.label("IN", Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(150f).left();
        head.add().width(22f);
        head.add(ui.label("OUT · MAIN FIRST", Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(170f).left();
        card.add(head).row();
        card.add(new Image(ui.solid(Theme.NAVY))).height(Theme.BORDER_THIN).row();
        List<String> groups = new java.util.ArrayList<>();
        for (HandsOnLevel.Resource resource : level.resources()) {
            if (!resource.id().equals(level.payer().id())) {
                groups.add(resource.id());
            }
        }
        groups.add(level.payer().id());
        for (String group : groups) {
            List<HandsOnLevel.Command> commands = level.commands().stream()
                .filter(command -> command.group().equals(group)).toList();
            if (commands.isEmpty()) {
                continue;
            }
            Table block = new Table();
            Table groupTile = new Table();
            groupTile.add(tile(level, group, 40f)).row();
            groupTile.add(ui.label(level.resource(group).orElseThrow().name(), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).padTop(4f);
            block.add(groupTile).width(84f).top().padTop(8f).padLeft(10f);
            Table rows = new Table();
            rows.defaults().growX();
            for (HandsOnLevel.Command command : commands) {
                rows.add(commandRow(level, state, command, busy)).row();
            }
            block.add(rows).width(0f).expandX().fillX();
            if (busy) {
                block.getColor().a = 0.55f;
            }
            card.add(block).row();
            card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(Theme.BORDER_THIN).row();
        }
        card.add(ui.paragraph("Inputs are paid when you queue a command, outputs come at execution, so what a "
            + "command makes can't pay for another this round. A fail costs 1 of the main output; an exceptional "
            + "result adds 1.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).pad(8f, 16f, 12f, 16f).row();
        return card;
    }

    private Table commandRow(HandsOnLevel level, HandsOnState state, HandsOnLevel.Command command, boolean busy) {
        String shortOf = HandsOnView.shortOf(level, HandsOn.shortOf(level, state, command));
        boolean can = !busy && HandsOn.canQueue(level, state, command);
        Table row = new Table();
        row.pad(9f, 12f, 9f, 16f);
        if (!busy && shortOf != null) {
            row.setBackground(ui.cardAccent(Theme.DANGER));
        }
        Table name = new Table();
        name.left();
        name.defaults().left();
        name.add(ui.label(command.name(), Theme.TextStyle.BODY_STRONG, can || busy ? Theme.INK : Theme.INK_MUTED))
            .row();
        if (!busy && shortOf != null) {
            name.add(ui.label(shortOf, Theme.TextStyle.CAPTION, Theme.DANGER));
        } else if (!command.rolls()) {
            name.add(ui.label("Never rolls; uses a slot", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        row.add(name).width(0f).expandX().fillX().left();
        Table in = amounts(level, command.input());
        if (shortOf != null && !busy) {
            in.getColor().a = 0.5f;
        }
        row.add(in).width(150f).left();
        row.add(ui.label("›", Theme.TextStyle.BODY_STRONG, Theme.INK_MUTED)).width(22f);
        row.add(amounts(level, command.output())).width(170f).left();
        if (can) {
            row.setTouchable(Touchable.enabled);
            row.addListener(clicked(() -> step(RdDecisions.HandsOnStep.QUEUE, command.id())));
        }
        return row;
    }

    // ---- The queue panel

    private Table queuePanel(HandsOnLevel level, HandsOnState state) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().padBottom(12f);
        boolean awaiting = state.phase() == HandsOnState.Phase.AWAITING_RESIST;
        boolean results = awaiting || showingRound > 0;
        List<String> queue = results ? state.results().stream().map(HandsOnState.Slot::command).toList()
            : state.queue();
        List<String> shown = awaiting ? state.queue() : queue;
        Table head = new Table();
        head.add(ui.label("Commands " + shown.size() + " / " + level.slots(), Theme.TextStyle.HEADING_SM, Theme.INK))
            .expandX().left();
        TextButton undo = actionButton("Undo", Theme.ButtonKind.SECONDARY, () -> step(RdDecisions.HandsOnStep.UNDO,
            null));
        TextButton reset = actionButton("Reset", Theme.ButtonKind.SECONDARY, () -> step(RdDecisions.HandsOnStep.RESET,
            null));
        undo.setDisabled(results || state.queue().isEmpty());
        reset.setDisabled(results || state.queue().isEmpty());
        head.add(undo).padRight(8f);
        head.add(reset);
        card.add(head).row();
        for (int i = 0; i < level.slots(); i++) {
            card.add(slot(level, state, i, shown, awaiting, results)).minHeight(72f).row();
        }
        card.add(odds(state)).row();
        card.add().expandY().row();
        Table foot = new Table();

        foot.add(new Image(ui.solid(Theme.NAVY))).colspan(2).height(Theme.BORDER_THIN).growX().padBottom(12f).row();
        String round = showingRound > 0 ? "Round " + showingRound + " done" : "Round " + state.round() + " of "
            + level.rounds();
        foot.add(ui.label(round.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.INK)).expandX().left();
        TextButton confirm;
        if (showingRound > 0) {
            confirm = actionButton("Plan round " + state.round(), Theme.ButtonKind.PRIMARY, () -> {
                showingRound = 0;
                rebuild();
            });
        } else {
            confirm = actionButton(awaiting ? "Executing…" : "Confirm commands", Theme.ButtonKind.COMMIT,
                () -> step(RdDecisions.HandsOnStep.CONFIRM, null));
            confirm.setDisabled(awaiting);
        }
        foot.add(confirm).right();
        card.add(foot).padBottom(0f).row();
        return card;
    }

    private Table slot(HandsOnLevel level, HandsOnState state, int index, List<String> shown, boolean awaiting,
            boolean results) {
        Table slot = new Table();
        if (index >= shown.size()) {
            slot.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
            slot.pad(12f, 14f, 12f, 14f);
            slot.add(number(index + 1, false)).padRight(12f);
            slot.add(ui.label(results ? "Empty" : "Empty · click a command to queue it", Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).expandX().left();
            return slot;
        }
        HandsOnLevel.Command command = level.command(shown.get(index)).orElseThrow();
        HandsOnState.Slot result = results && index < state.results().size() ? state.results().get(index) : null;
        boolean waitingFail = awaiting && index == state.slot();
        boolean waitsBehind = awaiting && index > state.slot();
        Color border = waitingFail || result != null && result.rolled() == HandsOnState.Roll.FAIL && !result.resisted()
            ? Theme.DANGER : result != null && result.rolled() == HandsOnState.Roll.EXCEPTIONAL ? Theme.SUCCESS
            : result != null ? Theme.NAVY : Theme.NAVY_BORDER;
        slot.setBackground(ui.rect(Theme.SURFACE_CARD, border, border == Theme.NAVY_BORDER ? Theme.BORDER_THIN
            : Theme.BORDER_EMPHASIS));
        slot.pad(12f, 14f, 12f, 14f);
        slot.add(number(index + 1, true)).padRight(12f);
        slot.add(tile(level, command.group(), 34f)).padRight(12f);
        Table words = new Table();
        words.left();
        words.defaults().left();
        words.add(ui.label(command.name(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        String line;
        if (waitingFail) {
            int base = command.output().get(command.primary());
            line = "Makes " + Math.max(0, base - 1) + " instead of " + base;
        } else if (waitsBehind) {
            line = "Waits for your answer";
        } else if (result != null) {
            line = "Made " + HandsOnView.target(level, result.gained());
        } else {
            line = "Paid " + HandsOnView.target(level, command.input()) + " · makes "
                + HandsOnView.target(level, command.output());
            if (command.input().isEmpty()) {
                line = "Free · makes " + HandsOnView.target(level, command.output());
            }
        }
        words.add(ui.label(line, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        slot.add(words).expandX().fillX().left();
        String badge = waitingFail ? "Failed −1" : HandsOnView.badge(result, waitsBehind);
        slot.add(badge(badge)).right().top();
        if (waitingFail) {
            slot.row();
            Table ask = new Table();
            ask.add(ui.paragraph("A fail. Call in an expert to make it a normal result?", Theme.TextStyle.CAPTION,
                Theme.INK)).expandX().fillX().left();
            ask.add(actionButton("Accept", Theme.ButtonKind.SECONDARY, () -> step(RdDecisions.HandsOnStep.ACCEPT,
                null))).padRight(8f);
            TextButton expert = actionButton("Call in an expert (" + level.resist() + " "
                + level.payer().one() + ")", Theme.ButtonKind.PRIMARY, () -> step(RdDecisions.HandsOnStep.RESIST, null));
            expert.setDisabled(state.held(level.payer().id()) < level.resist());
            ask.add(expert);
            slot.add(ask).colspan(4).growX().padTop(10f);
        }
        return slot;
    }

    private Table number(int n, boolean filled) {
        Table circle = new Table();
        circle.setBackground(ui.circle(Theme.SURFACE_CARD, filled ? Theme.NAVY : Theme.NAVY_BORDER,
            Theme.BORDER_EMPHASIS, 26));
        circle.add(ui.label(String.valueOf(n), Theme.TextStyle.LABEL, filled ? Theme.NAVY : Theme.INK_MUTED));
        Table wrap = new Table();
        wrap.add(circle).size(26f);
        return wrap;
    }

    private Table badge(String text) {
        Color fill = text.startsWith("Failed") ? Theme.DANGER : text.startsWith("Exceptional") ? Theme.SUCCESS
            : text.startsWith("Done") ? Theme.NAVY : null;
        Table badge = new Table();
        badge.setBackground(fill == null ? ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN)
            : ui.solid(fill));
        badge.pad(3f, 8f, 3f, 8f);
        badge.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, fill == null ? Theme.INK_MUTED
            : fill == Theme.NAVY ? Theme.ON_FILL : Theme.SURFACE_CARD));
        return badge;
    }

    private Table odds(HandsOnState state) {
        Table odds = new Table();
        odds.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.04f), Theme.NAVY_BORDER,
            Theme.BORDER_THIN));
        odds.pad(10f, 12f, 10f, 12f);
        odds.defaults().left().growX();
        long fail = HandsOnView.percent(state.failChance());
        long exceptional = HandsOnView.percent(state.exceptionalChance());
        long done = 100 - fail - exceptional;
        Table bar = new Table();
        bar.add(new Image(ui.solid(Theme.DANGER))).height(8f).width(com.badlogic.gdx.scenes.scene2d.ui.Value
            .percentWidth(fail / 100f, bar));
        bar.add(new Image(ui.solid(Theme.NAVY))).height(8f).width(com.badlogic.gdx.scenes.scene2d.ui.Value
            .percentWidth(done / 100f, bar));
        bar.add(new Image(ui.solid(Theme.SUCCESS))).height(8f).width(com.badlogic.gdx.scenes.scene2d.ui.Value
            .percentWidth(exceptional / 100f, bar));
        odds.add(bar).padBottom(6f).row();
        MissionComponent component = session.catalog().missionComponent(componentId).orElseThrow();
        SetSpecialist lead = HandsOnView.lead(session.state(), component);
        odds.add(ui.paragraph("Each command rolls: " + fail + " % fail (−1 main output) · " + done + " % done · "
            + exceptional + " % exceptional (+1)." + (lead == null ? "" : " " + People.surname(lead.getName())
            + " leads (" + SetSkills.label(component.associatedSkill()) + " "
            + Math.round(lead.getSkill(component.associatedSkill())) + ")."), Theme.TextStyle.CAPTION, Theme.INK))
            .row();
        odds.add(ui.paragraph("On a fail with shifts left you may call in an expert for 1 shift.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return odds;
    }

    // ---- The result

    private Table resultPanel(HandsOnLevel level, Decisions.Decision decision, HandsOnState state) {
        HandsOnState.Result result = state.result();
        Color accent = result == HandsOnState.Result.BONUS ? Theme.GOLD : result == HandsOnState.Result.SUCCESS
            ? Theme.SUCCESS : Theme.DANGER;
        Table panel = new Table();
        panel.setBackground(ui.rect(Theme.SURFACE_CARD, accent, Theme.BORDER_EMPHASIS));
        panel.top();
        panel.defaults().growX().left();
        panel.add(new Image(ui.solid(accent))).height(4f).row();
        Table body = new Table();
        body.pad(20f);
        body.top();
        body.defaults().growX().left().padBottom(14f);
        boolean early = result == HandsOnState.Result.BONUS && state.resultsRound() < level.rounds();
        body.add(ui.label((early ? "Campaign over · ended early in round " + state.resultsRound()
            : "Campaign over · all " + level.rounds() + " rounds").toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW,
            Theme.INK_MUTED)).padBottom(4f).row();
        body.add(ui.label(HandsOnView.result(result), Theme.TextStyle.HEADING_LG, Theme.INK)).row();
        Table table = new Table();
        String[] heads = {"HELD AT THE END", "HELD", "OBJECTIVE", "BONUS"};
        for (int i = 0; i < heads.length; i++) {
            table.add(ui.label(heads[i], Theme.TextStyle.LABEL, Theme.INK_MUTED)).align(i == 0
                ? com.badlogic.gdx.utils.Align.left : com.badlogic.gdx.utils.Align.right).expand(i == 0, false)
                .width(i == 0 ? 0f : 70f).padBottom(6f);
        }
        table.row();
        table.add(new Image(ui.solid(Theme.NAVY))).colspan(4).height(Theme.BORDER_THIN).growX().padBottom(6f).row();
        level.objective().forEach((resource, target) -> {
            int held = state.held(resource);
            int bonus = level.bonus().getOrDefault(resource, target);
            table.add(ui.label(level.resource(resource).orElseThrow().name(), Theme.TextStyle.BODY_SM, Theme.INK))
                .left().padBottom(6f);
            table.add(ui.label(String.valueOf(held), Theme.TextStyle.HEADING_SM, held >= target ? Theme.SUCCESS
                : Theme.DANGER)).right().padBottom(6f);
            table.add(ui.label(String.valueOf(target), Theme.TextStyle.BODY_STRONG, Theme.INK_MUTED)).right()
                .padBottom(6f);
            table.add(ui.label(String.valueOf(bonus), Theme.TextStyle.BODY_STRONG, Theme.INK_MUTED)).right()
                .padBottom(6f);
            table.row();
        });
        body.add(table).row();
        body.add(ui.label("THE EFFECT", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(2f).row();
        String effect = HandsOnView.worth(decision, result);
        var applied = session.state().getPendingDecision().effects().stream()
            .filter(e -> e.kind() == de.mkoehler.launchwindow.model.DecisionEffect.Kind.RELIABILITY).findFirst();
        body.add(ui.paragraph(effect + (applied.isPresent() ? String.format(Locale.ROOT, ": reliability +%.1f",
            applied.get().amount()) : ": reliability unchanged"), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        body.add(ui.paragraph(result == HandsOnState.Result.BONUS ? "The bonus ends the campaign as soon as it is met."
            : result == HandsOnState.Result.SUCCESS ? "The objective met, the bonus not."
            : "Falling short costs nothing but the chance; the R&D goes on as before.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).row();
        body.add(actionButton("Back to the R&D board", Theme.ButtonKind.PRIMARY, this::goBack)).left()
            .fill(false, false);
        panel.add(body).grow();
        return panel;
    }

    // ---- Steps

    private void step(RdDecisions.HandsOnStep step, String command) {
        int roundBefore = session.state().getPendingDecision() == null ? 0
            : session.state().getPendingDecision().handsOn().round();
        submit(new HandsOnAction(step, command), result -> {
            PendingDecision pending = session.state().getPendingDecision();
            if (pending != null && pending.handsOn() != null && pending.handsOn().phase()
                    == HandsOnState.Phase.PLANNING && pending.handsOn().round() > roundBefore) {
                // A round ran: show its results until the player moves on.
                showingRound = roundBefore;
                rebuild();
            }
        });
    }

    /**
     * Takes a step as its button or row would, for the offscreen tools (the hands-on playground's smoke
     * test).
     *
     * @param step    the step
     * @param command the command, for {@link RdDecisions.HandsOnStep#QUEUE}, else {@code null}
     */
    public void take(RdDecisions.HandsOnStep step, String command) {
        step(step, command);
    }

    /**
     * Shows a round's results as just executed, for the offscreen screenshot tool.
     *
     * @param round the round
     */
    public void showRound(int round) {
        showingRound = round;
        rebuild();
    }
}
