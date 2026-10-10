package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.DecideAction;
import de.mkoehler.launchwindow.actions.PullOutOfTrainingAction;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Concepts;
import de.mkoehler.launchwindow.client.view.DecisionView;
import de.mkoehler.launchwindow.client.view.Explainers;
import de.mkoehler.launchwindow.client.view.Outlook;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.DecisionEffect;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.RdDecisions;
import de.mkoehler.launchwindow.rules.ResearchPlanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The R&amp;D decision's banner inside a component's R&amp;D card ({@code ui-spec.md} component 26, the
 * "R&amp;D decisions" canvas, 2026-10-10): open, with the story, the type's panel and the options;
 * decided, with the outcome; or folded to one line. Choosing opens a confirmation, and deciding goes
 * through the host's submit, which brings the outcome back at once.
 *
 * @author Mario Koehler
 */
final class DecisionBanner {

    /** Outcomes folded to one line, by decision and turn, for as long as the client runs. */
    private static final Set<String> FOLDED = new HashSet<>();

    private final ChromeScreen host;
    private final UiKit ui;
    private final boolean editable;

    /**
     * Creates the banner for a screen.
     *
     * @param host     the screen it sits on
     * @param editable whether the player can decide here; read-only elsewhere, with a link to the board
     */
    DecisionBanner(ChromeScreen host, boolean editable) {
        this.host = host;
        this.ui = host.ui;
        this.editable = editable;
    }

    /**
     * Builds the banner for a component's card, or returns {@code null} when no decision is about it.
     *
     * @param component the component
     * @return the banner, or {@code null}
     */
    Table build(MissionComponent component) {
        PlayerSnapshot state = host.session.state();
        PendingDecision pending = DecisionView.about(state, component.id()).orElse(null);
        Decisions.Decision decision = pending == null ? null : DecisionView.content(pending).orElse(null);
        if (decision == null) {
            return null;
        }
        if (!pending.decided()) {
            return open(pending, decision, component);
        }
        return FOLDED.contains(key(pending)) ? folded(pending, decision) : decided(pending, decision);
    }

    private static String key(PendingDecision pending) {
        return pending.decisionId() + "@" + pending.offeredTurn();
    }

    // ---- Open

    private Table open(PendingDecision pending, Decisions.Decision decision, MissionComponent component) {
        PlayerSnapshot state = host.session.state();
        Table banner = new Table();
        banner.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.10f), Theme.GOLD,
            Theme.BORDER_EMPHASIS));
        banner.pad(14f, 16f, 14f, 16f);
        banner.defaults().growX().left();

        Table top = new Table();
        top.add(ui.label(DecisionView.eyebrow(decision, pending.componentId()), Theme.TextStyle.EYEBROW,
            Theme.GOLD_TEXT)).left();
        Image help = new Image(ui.icon("help"), Scaling.fit);
        help.setColor(Theme.GOLD_TEXT);
        help.setTouchable(Touchable.enabled);
        help.addListener(ChromeScreen.clicked(host::openHelp));
        ui.tooltip(help, "Decisions", "How R&D decisions work: open the help on Decisions.");
        top.add(help).size(13f).padLeft(6f).left();
        top.add().expandX();
        Image clock = new Image(ui.icon("clock"), Scaling.fit);
        clock.setColor(Theme.INK_MUTED);
        top.add(clock).size(12f).padRight(5f);
        top.add(ui.label(DecisionView.untilResolved(host.nextTurnText()), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        banner.add(top).padBottom(10f).row();

        banner.add(ui.paragraph(decision.headline(), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
        banner.add(ui.paragraph(decision.text(), Theme.TextStyle.BODY, Theme.INK)).padBottom(10f).row();
        if (decision.step() > 1) {
            banner.add(ui.paragraph(decision.recall() + " · Step " + decision.step() + " of up to "
                + DecisionView.arcLength(decision), Theme.TextStyle.CAPTION, Theme.GOLD_TEXT)).padBottom(10f).row();
        }
        if (!decision.clues().isEmpty()) {
            banner.add(clues(decision)).padBottom(10f).row();
        }
        if (decision.type() == Decisions.Type.INTEL) {
            banner.add(intelLine(pending, decision)).padBottom(10f).row();
        }
        if (!decision.stakes().isEmpty()) {
            banner.add(ui.paragraph(decision.stakes(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(6f).row();
        }
        Table options = new Table();
        options.defaults().growX().padBottom(6f);
        for (Decisions.Option option : decision.options()) {
            if (option.takes() == Decisions.Takes.OVERSEER) {
                overseerRows(options, pending, decision, option, component);
                continue;
            }
            String shows = DecisionView.shows(option, state);
            String preview = decision.type() == Decisions.Type.TRADEOFF ? preview(pending, option) : null;
            options.add(optionRow(option.label(), shows, preview, option.id().equals(decision.defaultOption()), false,
                () -> confirm(pending, decision, option, null))).row();
        }
        banner.add(options).row();
        if (!decision.explainers().isEmpty()) {
            Table chips = new Table();
            chips.left();
            chips.add(ui.label("How it works:", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(Theme.SPACE_2);
            List<Concepts.Concept> concepts = new ArrayList<>();
            for (String id : decision.explainers()) {
                Explainers.standard().byId(id).ifPresent(explainer -> concepts.add(new Concepts.Concept(id,
                    explainer.title(), id, 1, null, null, null, null, null)));
            }
            chips.add(host.conceptChips(concepts, () -> host)).left();
            banner.add(chips).padTop(4f).row();
        }
        if (!editable) {
            Label link = ui.label("Decide on the R&D board ›", Theme.TextStyle.CAPTION, Theme.NAVY);
            link.setTouchable(Touchable.enabled);
            link.addListener(ChromeScreen.clicked(() -> host.game.setScreen(new RndBoardScreen(host.game,
                DecisionView.rocket(pending.componentId(), host.session.catalog()) ? RndBoardScreen.Board.ROCKET
                    : RndBoardScreen.Board.PAYLOAD))));
            banner.add(link).padTop(6f).row();
        }
        return banner;
    }

    /** One option row: the label, what it shows, a preview, the default tag and a chevron. */
    private Table optionRow(String label, String shows, String preview, boolean isDefault, boolean muted,
                           Runnable onClick) {
        Drawable plain = ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN);
        Drawable hover = ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.04f), Theme.NAVY, Theme.BORDER_THIN);
        Table row = new Table();
        row.setBackground(plain);
        row.pad(9f, 12f, 9f, 12f);
        Table words = new Table();
        words.left();
        words.defaults().left().growX();
        words.add(ui.paragraph(label, Theme.TextStyle.BODY_STRONG, muted ? Theme.INK_MUTED : Theme.INK)).row();
        if (shows != null && !shows.isEmpty()) {
            words.add(ui.paragraph(shows, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padTop(1f).row();
        }
        if (preview != null) {
            words.add(ui.paragraph(preview, Theme.TextStyle.CAPTION, Theme.GOLD_TEXT)).padTop(1f).row();
        }
        row.add(words).expandX().fillX().left();
        if (isDefault) {
            Table tag = new Table();
            tag.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            tag.pad(2f, 6f, 2f, 6f);
            tag.add(ui.label("IF YOU DON'T DECIDE", Theme.TextStyle.LABEL, Theme.INK_MUTED));
            row.add(tag).padLeft(Theme.SPACE_3);
        }
        if (editable && onClick != null) {
            Image chevron = new Image(ui.icon("chevron"), Scaling.fit);
            chevron.setColor(Theme.NAVY);
            row.add(chevron).size(14f).padLeft(Theme.SPACE_3);
            row.setTouchable(Touchable.enabled);
            row.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer,
                                  com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    row.setBackground(hover);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer,
                                 com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                    row.setBackground(plain);
                }

                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onClick.run();
                }
            });
        }
        return row;
    }

    /** Who oversees it: the idle engineers as rows with their chance, then those in training, muted. */
    private void overseerRows(Table options, PendingDecision pending, Decisions.Decision decision,
                              Decisions.Option option, MissionComponent component) {
        PlayerSnapshot state = host.session.state();
        List<SetSpecialist> idle = RdDecisions.overseers(state, component);
        String skill = SetSkills.label(component.associatedSkill());
        for (SetSpecialist engineer : idle) {
            double chance = RdDecisions.goodChance(state, host.session.catalog(), pending, option, engineer.getId(),
                null);
            String shows = String.format(Locale.ROOT, "%s %d · %.0f %% chance it goes well", skill,
                Math.round(engineer.getSkill(component.associatedSkill())), chance * 100);
            options.add(optionRow(engineer.getName(), shows, null, false, false,
                () -> confirm(pending, decision, option, engineer.getId()))).row();
        }
        for (SetSpecialist engineer : state.getSetSpecialists()) {
            if (engineer.getAssignmentState() != AssignmentState.IN_TRAINING
                    || engineer.getTrainingKind() == TrainingKind.BASIC) {
                continue;
            }
            Table row = optionRow(engineer.getName(), skill + " " + Math.round(engineer.getSkill(
                component.associatedSkill())) + " · in training", null, false, true, null);
            if (editable) {
                Label pull = ui.label("Pull out of training to choose", Theme.TextStyle.CAPTION, Theme.NAVY);
                pull.setTouchable(Touchable.enabled);
                pull.addListener(ChromeScreen.clicked(() -> confirmPullOut(engineer)));
                row.add(pull).padLeft(Theme.SPACE_3);
            }
            options.add(row).row();
        }
        if (idle.isEmpty()) {
            options.add(ui.paragraph("Nobody is idle to oversee it.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        }
    }

    /** A diagnosis's clues, each with its icon and kind. */
    private Table clues(Decisions.Decision decision) {
        Table clues = new Table();
        clues.left();
        clues.defaults().left().padBottom(6f);
        for (Decisions.Clue clue : decision.clues()) {
            Image icon = new Image(ui.icon(clue.icon() == null ? "clue_note" : clue.icon()), Scaling.fit);
            icon.setColor(Theme.NAVY);
            clues.add(icon).size(16f).top().padRight(Theme.SPACE_2).padTop(2f);
            Table words = new Table();
            words.left();
            words.defaults().left().growX();
            words.add(ui.label(clue.kind().toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).row();
            words.add(ui.paragraph(clue.text(), Theme.TextStyle.BODY_SM, Theme.INK));
            clues.add(words).growX().row();
        }
        return clues;
    }

    /** The Intelligence Office's level and how far its tip can be trusted. */
    private Table intelLine(PendingDecision pending, Decisions.Decision decision) {
        PlayerSnapshot state = host.session.state();
        int level = state.getIntelligence().getLevel();
        Decisions.Option adopt = decision.options().stream().filter(o -> o.odds() == Decisions.Odds.INTEL)
            .findFirst().orElse(null);
        double chance = adopt == null ? 0 : RdDecisions.goodChance(state, host.session.catalog(), pending, adopt, null,
            null);
        String trust = chance >= 0.8 ? "almost always right about this" : chance >= 0.6 ? "usually right about this"
            : chance >= 0.45 ? "right about as often as not" : "often wrong about this";
        Table line = new Table();
        line.left();
        Image icon = new Image(ui.icon("binoculars"), Scaling.fit);
        icon.setColor(Theme.NAVY);
        line.add(icon).size(16f).padRight(Theme.SPACE_2);
        line.add(ui.paragraph(String.format(Locale.ROOT, "Intelligence Office, level %d: our sources are %s "
            + "(%.0f %%).", level, trust, chance * 100), Theme.TextStyle.BODY_SM, Theme.INK)).growX();
        return line;
    }

    /**
     * A trade-off option's preview: the seasons to 60 % and to the cap with it, against without.
     */
    private String preview(PendingDecision pending, Decisions.Option option) {
        if (option.outcomes().size() != 1) {
            return null;
        }
        double seasons = 0;
        double factor = 1;
        int factorSeasons = 0;
        for (Decisions.Effect effect : option.outcomes().get(0).effects()) {
            if (effect.kind() == DecisionEffect.Kind.RELIABILITY) {
                seasons += effect.amount();
            } else if (effect.kind() == DecisionEffect.Kind.RD_FACTOR) {
                factor = effect.amount();
                factorSeasons = effect.seasons();
            }
        }
        if (seasons == 0 && factorSeasons == 0) {
            return null;
        }
        PlayerSnapshot state = host.session.state();
        double gain = Math.max(1.0, ResearchPlanning.expectedSeasonGain(state, host.session.catalog(),
            pending.componentId()));
        ResearchPlanning.Projection now = ResearchPlanning.project(state, host.session.catalog(),
            pending.componentId());
        ResearchPlanning.Projection then = ResearchPlanning.projectWith(state, host.session.catalog(),
            pending.componentId(), seasons * gain, factor, factorSeasons);
        // The next mark ahead: 60 % while below it, else the next milestone, else the cap.
        double mark = now.cap();
        for (double milestone : Outlook.MILESTONES) {
            if (milestone > now.start() && milestone < now.cap()) {
                mark = milestone;
                break;
            }
        }
        String name = mark == now.cap() ? "the cap" : String.format(Locale.ROOT, "%.0f %%", mark);
        String line = "With this: " + name + " in " + Outlook.approx(then.seasonsTo(mark)) + " seasons (now "
            + Outlook.approx(now.seasonsTo(mark)) + ")";
        // The cap too, where the trade pays back, while either gets there within the projection.
        if (mark != now.cap() && (then.seasonsTo(then.cap()).isPresent() || now.seasonsTo(now.cap()).isPresent())) {
            line += " · the cap in " + Outlook.approx(then.seasonsTo(then.cap())) + " (now "
                + Outlook.approx(now.seasonsTo(now.cap())) + ")";
        }
        return line;
    }

    // ---- Decided

    private Table decided(PendingDecision pending, Decisions.Decision decision) {
        PlayerSnapshot state = host.session.state();
        Decisions.Outcome outcome = decision.outcome(pending.outcomeId()).orElse(null);
        Color tone = tone(outcome);
        Table banner = new Table();
        banner.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(tone, 0.07f), tone, Theme.BORDER_EMPHASIS));
        banner.pad(14f, 16f, 14f, 16f);
        banner.defaults().growX().left();
        Table top = new Table();
        top.add(toneMark(outcome)).size(22f).padRight(Theme.SPACE_2);
        String option = decision.option(pending.optionId()).map(Decisions.Option::label).orElse("");
        top.add(ui.label(("Decided · " + option).toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, tone))
            .expandX().left();
        Image fold = new Image(ui.icon("close"), Scaling.fit);
        fold.setColor(Theme.INK_MUTED);
        fold.setTouchable(Touchable.enabled);
        fold.addListener(ChromeScreen.clicked(() -> {
            FOLDED.add(key(pending));
            host.rebuild();
        }));
        top.add(fold).size(12f);
        banner.add(top).padBottom(8f).row();
        if (outcome != null) {
            banner.add(ui.paragraph(DecisionView.fill(outcome.headline(), state, pending.personId(),
                pending.secondPersonId()), Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(4f).row();
            banner.add(ui.paragraph(DecisionView.fill(outcome.text(), state, pending.personId(),
                pending.secondPersonId()), Theme.TextStyle.BODY, Theme.INK)).padBottom(8f).row();
        }
        for (DecisionView.EffectLine line : DecisionView.effects(pending.effects(), state)) {
            banner.add(ui.label(line.text(), Theme.TextStyle.BODY_STRONG, line.good() ? Theme.SUCCESS : Theme.DANGER))
                .padBottom(2f).row();
        }
        Table foot = new Table();
        foot.left();
        if (outcome != null && outcome.continues() && decision.followUp() != null) {
            foot.add(ui.badge(Theme.BadgeKind.ACTIVE, "To be continued")).padRight(Theme.SPACE_3);
        }
        foot.add(ui.label("Stays here until the turn resolves.", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        banner.add(foot).padTop(6f).row();
        return banner;
    }

    private Table folded(PendingDecision pending, Decisions.Decision decision) {
        Decisions.Outcome outcome = decision.outcome(pending.outcomeId()).orElse(null);
        Color tone = tone(outcome);
        Table line = new Table();
        line.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(tone, 0.07f), tone, Theme.BORDER_THIN));
        line.pad(6f, 10f, 6f, 10f);
        line.add(toneMark(outcome)).size(16f).padRight(Theme.SPACE_2);
        line.add(ui.label("Decided: " + (outcome == null ? decision.headline() : DecisionView.fill(outcome.headline(),
            host.session.state(), pending.personId(), pending.secondPersonId())), Theme.TextStyle.BODY_SM, Theme.INK))
            .expandX().left();
        Label show = ui.label("Show", Theme.TextStyle.CAPTION, Theme.NAVY);
        show.setTouchable(Touchable.enabled);
        show.addListener(ChromeScreen.clicked(() -> {
            FOLDED.remove(key(pending));
            host.rebuild();
        }));
        line.add(show);
        return line;
    }

    private static Color tone(Decisions.Outcome outcome) {
        if (outcome == null) {
            return Theme.NAVY;
        }
        return switch (outcome.tone()) {
            case GOOD -> Theme.SUCCESS;
            case BAD -> Theme.DANGER;
            case NEUTRAL -> Theme.NAVY;
        };
    }

    /** The round tone mark: a tick, a cross, or a dot for neutral, on the tone's colour. */
    private Table toneMark(Decisions.Outcome outcome) {
        Color tone = tone(outcome);
        Table mark = new Table();
        mark.setBackground(ui.solid(tone));
        String icon = outcome == null || outcome.tone() == Decisions.Tone.NEUTRAL ? "check"
            : outcome.tone() == Decisions.Tone.GOOD ? "check" : "close";
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.ON_FILL);
        mark.add(glyph).size(12f);
        return mark;
    }

    // ---- Choosing

    /** The people a confirmation picks, kept while it's rebuilt. */
    private String firstPick;
    private String secondPick;

    /**
     * Opens the confirmation for an option of the waiting decision, as clicking it does.
     *
     * @param optionId the option
     * @param person   the person it takes, or {@code null}
     */
    void confirm(String optionId, String person) {
        PendingDecision pending = host.session.state().getPendingDecision();
        Decisions.Decision decision = pending == null ? null : DecisionView.content(pending).orElse(null);
        Decisions.Option option = decision == null ? null : decision.option(optionId).orElse(null);
        if (option != null) {
            confirm(pending, decision, option, person);
        }
    }

    private void confirm(PendingDecision pending, Decisions.Decision decision, Decisions.Option option,
                         String person) {
        firstPick = person;
        secondPick = null;
        showConfirm(pending, decision, option);
    }

    private void showConfirm(PendingDecision pending, Decisions.Decision decision, Decisions.Option option) {
        PlayerSnapshot state = host.session.state();
        MissionComponent component = host.session.catalog().missionComponent(pending.componentId()).orElseThrow();
        Table card = host.dialogCard("Decision", option.label());
        String shows = DecisionView.shows(option, state);
        if (!shows.isEmpty()) {
            card.add(ui.paragraph(shows + ".", Theme.TextStyle.BODY, Theme.INK_MUTED)).growX().padBottom(Theme.SPACE_3)
                .row();
        }
        boolean ready = true;
        switch (option.takes()) {
            case OVERSEER -> {
                SetSpecialist engineer = engineer(state, firstPick);
                if (engineer != null) {
                    card.add(ui.paragraph(String.format(Locale.ROOT, "Overseen by %s (%s %d): %.0f %% chance it goes "
                        + "well.", engineer.getName(), SetSkills.label(component.associatedSkill()),
                        Math.round(engineer.getSkill(component.associatedSkill())), 100 * RdDecisions.goodChance(state,
                            host.session.catalog(), pending, option, firstPick, null)), Theme.TextStyle.BODY,
                        Theme.INK)).growX().padBottom(Theme.SPACE_3).row();
                }
            }
            case SECONDED -> {
                card.add(ui.label("WHO GOES?", Theme.TextStyle.LABEL, Theme.INK_MUTED)).left().padBottom(4f).row();
                card.add(picker(RdDecisions.team(state, component.id()), firstPick, component, id -> {
                    firstPick = id;
                    showConfirm(pending, decision, option);
                })).growX().padBottom(Theme.SPACE_3).row();
                ready = firstPick != null;
            }
            case PAIR -> {
                Table pickers = new Table();
                pickers.defaults().top().left().expandX().fillX();
                Table first = new Table();
                first.defaults().left().growX();
                first.add(ui.label("FIRST", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(4f).row();
                first.add(picker(RdDecisions.pairable(state, component), firstPick, component, id -> {
                    firstPick = id;
                    showConfirm(pending, decision, option);
                }));
                Table second = new Table();
                second.defaults().left().growX();
                second.add(ui.label("WORKS WITH", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(4f).row();
                List<SetSpecialist> others = RdDecisions.pairable(state, component).stream()
                    .filter(engineer -> !engineer.getId().equals(firstPick)).toList();
                second.add(picker(others, secondPick, component, id -> {
                    secondPick = id;
                    showConfirm(pending, decision, option);
                }));
                pickers.add(first).padRight(Theme.SPACE_4);
                pickers.add(second);
                card.add(pickers).growX().padBottom(Theme.SPACE_3).row();
                ready = firstPick != null && secondPick != null;
                if (ready) {
                    card.add(ui.paragraph(String.format(Locale.ROOT, "%.0f %% chance they work well together.",
                        100 * RdDecisions.goodChance(state, host.session.catalog(), pending, option, firstPick,
                            secondPick)), Theme.TextStyle.BODY, Theme.INK)).growX().padBottom(Theme.SPACE_3).row();
                }
            }
            default -> { }
        }
        long cost = DecisionView.cost(option, state);
        if (cost > 0) {
            card.add(ui.paragraph(String.format(Locale.ROOT, "Costs $ %,.1f. Funds $ %,.1f → $ %,.1f.", (double) cost,
                (double) state.getFunds(), (double) (state.getFunds() - cost)), Theme.TextStyle.BODY,
                state.getFunds() >= cost ? Theme.INK : Theme.DANGER)).growX().padBottom(Theme.SPACE_3).row();
        }
        card.add(ui.paragraph("Decided at once: the outcome shows on the card straight away.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).growX().padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(host.actionButton("Back", Theme.ButtonKind.SECONDARY, host::closeDialog)).padRight(10f);
        var decide = host.actionButton("Decide", Theme.ButtonKind.PRIMARY, () -> host.submit(new DecideAction(
            option.id(), firstPick, secondPick), result -> host.closeDialog()));
        decide.setDisabled(!ready);
        buttons.add(decide);
        card.add(buttons).right().fill(false, false);
        if (option.takes() == Decisions.Takes.PAIR) {
            host.showDialog(card, PAIR_DIALOG_WIDTH);
        } else {
            host.showDialog(card);
        }
    }

    /** Wide enough for two pickers side by side. */
    private static final float PAIR_DIALOG_WIDTH = 900f;

    /** A radio list of engineers: name and skill; the picked one marked. */
    private Table picker(List<SetSpecialist> people, String picked, MissionComponent component,
                         java.util.function.Consumer<String> onPick) {
        Table list = new Table();
        list.defaults().growX().padBottom(4f);
        if (people.isEmpty()) {
            list.add(ui.paragraph("Nobody fits.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            return list;
        }
        for (SetSpecialist engineer : people) {
            boolean on = engineer.getId().equals(picked);
            Table row = new Table();
            row.setBackground(on ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.06f), Theme.NAVY,
                Theme.BORDER_EMPHASIS) : ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            row.pad(6f, 10f, 6f, 10f);
            row.add(ui.label(engineer.getName(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            row.add(ui.label(SetSkills.label(component.associatedSkill()) + " " + Math.round(engineer.getSkill(
                component.associatedSkill())), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            row.setTouchable(Touchable.enabled);
            row.addListener(ChromeScreen.clicked(() -> onPick.accept(engineer.getId())));
            list.add(row).row();
        }
        return list;
    }

    private void confirmPullOut(SetSpecialist engineer) {
        Table card = host.dialogCard("Confirm", "Pull " + People.surname(engineer.getName()) + " out of training?");
        card.add(ui.paragraph("They keep partial credit for the round in progress and are free to oversee.",
            Theme.TextStyle.BODY, Theme.INK_MUTED)).growX().padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(host.actionButton("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).padRight(10f);
        buttons.add(host.actionButton("Pull out", Theme.ButtonKind.PRIMARY, () -> host.submit(
            new PullOutOfTrainingAction(PersonnelGroup.SET, engineer.getId()), result -> host.closeDialog())));
        card.add(buttons).right().fill(false, false);
        host.showDialog(card);
    }

    private static SetSpecialist engineer(PlayerSnapshot state, String id) {
        return id == null ? null : state.getSetSpecialists().stream().filter(e -> e.getId().equals(id)).findFirst()
            .orElse(null);
    }
}
