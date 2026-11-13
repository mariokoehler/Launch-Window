package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.AdjustSalariesAction;
import de.mkoehler.launchwindow.actions.MeetExpectationsAction;
import de.mkoehler.launchwindow.actions.RaiseSalaryAction;
import de.mkoehler.launchwindow.client.view.PayTexts;
import de.mkoehler.launchwindow.rules.PayExpectations;
import de.mkoehler.launchwindow.actions.DismissAction;
import de.mkoehler.launchwindow.actions.PullOutOfTrainingAction;
import de.mkoehler.launchwindow.actions.StartExpertTrainingAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.HistoryNotes;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.client.view.KeepBusy;
import de.mkoehler.launchwindow.client.view.Outlook;
import de.mkoehler.launchwindow.client.view.People;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.client.view.SetDepartment;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.DepartmentSalaries;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TrainingPlanning;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * A department's personnel dialogs, shared by the SET Centre and Mission Control: training
 * (component 12), pull out and dismiss confirmations (component 5), and Manage department
 * (component 13). Actions go through the host screen's submit loop.
 *
 * @author Mario Koehler
 */
final class PersonnelDialogs {

    /** The training dialog's width where Expert training makes a fourth kind, from the mockup. */
    private static final float EXPERT_DIALOG_WIDTH = 720f;

    private final ChromeScreen host;
    private final UiKit ui;
    private final Department department;

    /**
     * Creates the dialogs for a screen.
     *
     * @param host       the screen they open on
     * @param department the department
     */
    PersonnelDialogs(ChromeScreen host, Department department) {
        this.host = host;
        this.ui = host.ui;
        this.department = department;
    }

    private TextButton button(String text, Theme.ButtonKind kind, Runnable action) {
        TextButton button = ui.button(text, kind);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!button.isDisabled()) {
                    action.run();
                }
            }
        });
        return button;
    }

    /**
     * Opens the training dialog (component 12) for someone, or says they're busy.
     *
     * @param specialist the person
     */
    void openTraining(Personnel specialist) {
        if (SetDepartment.isBusy(specialist)) {
            host.toast("Busy right now.", Theme.NoticeKind.INFO);
            return;
        }
        new TrainingDialog(specialist).show();
    }

    /**
     * Opens the training dialog with a kind and skill already chosen, as after the player's clicks.
     *
     * @param specialist the person
     * @param kind       the training kind
     * @param skill      the skill's name
     */
    void openTraining(Personnel specialist, TrainingKind kind, String skill) {
        new TrainingDialog(specialist).choose(kind, skill).show();
    }

    /**
     * The training dialog's state and widgets: kind, skill, and for Permanent the target level.
     */
    final class TrainingDialog {
        private final Personnel specialist;
        private TrainingKind kind = TrainingKind.ADVANCED;
        private String skill;
        private int target;

        TrainingDialog(Personnel specialist) {
            this.specialist = specialist;
        }

        void show() {
            // Four kinds need the mockup's wider dialog; SET keeps the standard width.
            if (department.hasExperts()) {
                host.showDialog(build(), EXPERT_DIALOG_WIDTH);
            } else {
                host.showDialog(build());
            }
        }

        TrainingDialog choose(TrainingKind chosenKind, String chosenSkill) {
            kind = chosenKind;
            skill = chosenSkill;
            // Expert training only takes a skill at the threshold that isn't Expert yet.
            if (kind == TrainingKind.EXPERT && skill != null && !department.expertEligible(specialist).contains(skill)) {
                skill = null;
            }
            clampTarget();
            return this;
        }

        /** Keeps the target within what the server accepts, defaulting to 90 (the mockup's default). */
        private void clampTarget() {
            int min = minimumTarget();
            if (target < min) {
                target = Math.max(min, 90);
            }
            target = Math.min(target, (int) SkillGrowth.SKILL_CAP);
        }

        /** The lowest target the server accepts: above the current skill (TARGET_NOT_ABOVE_SKILL). */
        private int minimumTarget() {
            double current = skill == null ? 0 : department.skill(specialist, skill);
            return Math.min((int) SkillGrowth.SKILL_CAP, (int) Math.floor(current) + 1);
        }

        private Table build() {
            Table card = host.dialogCard("Training", "Start training — " + specialist.getName());
            card.add(ui.label("KIND", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
            Table segments = new Table();
            segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY, Theme.BORDER_THIN));
            segments.pad(Theme.BORDER_THIN);
            // No Basic: basic training only follows a hire, it isn't a player's choice.
            int advanced = TrainingProgress.ADVANCED_TRAINING_SEASONS;
            segments.add(segment("Advanced · " + seasons(advanced), TrainingKind.ADVANCED)).expandX().fillX()
                .uniformX();
            segments.add(segment("Permanent", TrainingKind.PERMANENT)).expandX().fillX().uniformX();
            if (department.hasExperts()) {
                segments.add(segment("Expert", TrainingKind.EXPERT)).expandX().fillX().uniformX();
            }
            card.add(segments).padBottom(Theme.SPACE_4).row();

            card.add(ui.label("SKILL", Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
            Table picks = new Table();
            picks.left();
            for (String option : department.skills()) {
                picks.add(skillPick(option)).size(department.hasExperts() ? 96f : 72f, 66f).padRight(Theme.SPACE_2);
            }
            card.add(picks).padBottom(Theme.SPACE_4).row();
            if (kind == TrainingKind.EXPERT) {
                card.add(expertInfo()).padBottom(Theme.SPACE_4).row();
            }

            if (kind == TrainingKind.PERMANENT) {
                Table head = new Table();
                head.add(ui.label("TARGET LEVEL", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX().left();
                Label value = ui.label(String.valueOf(target), Theme.TextStyle.BODY_SM, Theme.INK);
                head.add(value).right();
                card.add(head).padBottom(Theme.SPACE_2).row();
                Slider slider = new Slider(minimumTarget(), (float) SkillGrowth.SKILL_CAP, 1f, false, ui.slider());
                slider.setValue(target);
                slider.setDisabled(skill == null);
                // How long it takes, from the rules (the user's request, 2026-10-10).
                Label estimate = ui.paragraph(estimate(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED);
                slider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        target = Math.round(slider.getValue());
                        value.setText(String.valueOf(target));
                        estimate.setText(estimate());
                    }
                });
                card.add(slider).padBottom(Theme.SPACE_2).row();
                card.add(estimate).growX().padBottom(Theme.SPACE_4).row();
            }

            Table buttons = new Table();
            buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog))
                .padRight(10f);
            TextButton start = button("Start Training", Theme.ButtonKind.PRIMARY, this::confirm);
            start.setDisabled(skill == null);
            buttons.add(start);
            card.add(buttons).right().fill(false, false);
            return card;
        }

        private String seasons(int count) {
            return count + (count == 1 ? " season" : " seasons");
        }

        /**
         * Words how long permanent training to the target takes ({@link TrainingPlanning}): the
         * average seasons, the season it's done by and the spread of the draws, or where it stalls,
         * and a retirement that comes first.
         */
        private String estimate() {
            if (skill == null) {
                return "Pick a skill to see how long it takes.";
            }
            double current = department.skill(specialist, skill);
            double ceiling = department.ceiling(specialist, skill);
            TrainingPlanning.Estimate estimate = TrainingPlanning.permanent(specialist, current, ceiling, target);
            String text;
            if (estimate.reaches()) {
                TrainingPlanning.Range range = TrainingPlanning.range(specialist, current, ceiling, target);
                String spread = range.high() == Integer.MAX_VALUE ? "; in some runs it stalls short of it"
                    : range.low() == range.high() ? "" : "; " + range.low() + " to " + seasons(range.high())
                    + " in 8 runs of 10";
                text = "About " + seasons(estimate.seasons()) + " (" + estimate.rounds() + " "
                    + (estimate.rounds() == 1 ? "round" : "rounds") + " of " + TrainingProgress.ADVANCED_TRAINING_SEASONS
                    + "), done by " + Outlook.by(host.session.openTurn(), estimate.seasons()) + spread + ".";
            } else if (ceiling < SkillGrowth.SKILL_CAP) {
                text = String.format(Locale.ROOT, "Their other skills leave room up to %.1f, so training stops "
                    + "at about %.1f after %s.", Math.max(current, ceiling), estimate.finalSkill(),
                    seasons(estimate.seasons()));
            } else {
                text = String.format(Locale.ROOT, "Gains shrink toward %d, so training stops at about %.1f after %s.",
                    (int) SkillGrowth.SKILL_CAP, estimate.finalSkill(), seasons(estimate.seasons()));
            }
            int retires = PersonnelLifecycle.seasonsUntilRetirement(specialist, department.group());
            if (retires < estimate.seasons()) {
                text += " " + People.surname(specialist.getName()) + " retires in " + seasons(retires) + ", before that.";
            }
            return text;
        }

        private Table segment(String text, TrainingKind segmentKind) {
            boolean active = segmentKind == kind;
            Table segment = new Table();
            if (active) {
                segment.setBackground(ui.solid(Theme.NAVY));
            }
            segment.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
            segment.add(ui.label(text, Theme.TextStyle.LABEL, active ? Theme.ON_FILL : Theme.NAVY));
            segment.setTouchable(Touchable.enabled);
            segment.addListener(ChromeScreen.clicked(() -> {
                choose(segmentKind, skill);
                show();
            }));
            return segment;
        }

        /** What Expert training does, with this skill's per-season chance from the rules. */
        private Table expertInfo() {
            Table info = new Table();
            info.setBackground(ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.08f), Theme.GOLD,
                Theme.BORDER_THIN));
            info.pad(Theme.SPACE_3);
            info.defaults().left().growX();
            String title = skill == null
                ? department.isExpert(specialist) ? "Already an Expert."
                : department.expertEligible(specialist).isEmpty()
                    ? "No skill can train toward Expert yet."
                    : "Pick a skill at " + (int) SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD + " or more."
                : String.format(Locale.ROOT, "%.0f %% chance each season", SkillGrowth.expertTrainingSuccessChance(
                    department.skill(specialist, skill)));
            Table head = new Table();
            head.add(ui.label(title, Theme.TextStyle.BODY_STRONG, Theme.INK)).left();
            head.add(new HistoryNote(ui, HistoryNotes.EXPERT)).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2);
            head.add().expandX();
            info.add(head).padBottom(2f).row();
            info.add(ui.paragraph(String.format(Locale.ROOT, "Open-ended: it runs until the trait comes, %.0f %% a "
                + "season at %d rising to %.0f %% at %d. The skill doesn't grow, and pulling out gives nothing.",
                SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE, (int) SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD,
                SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE + SkillGrowth.EXPERT_TRAINING_SKILL_BONUS_MAX,
                (int) SkillGrowth.SKILL_CAP), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(2f).row();
            info.add(ui.paragraph("An Expert can save a failed step that relies on their skill: crew aboard, or "
                + "anyone at home, even off the mission. Each Expert helps at most three times a turn.",
                Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(2f).row();
            info.add(ui.paragraph("A person can only ever be an Expert in one skill, so choose it well.",
                Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            return info;
        }

        private Table skillPick(String option) {
            boolean active = option == skill;
            boolean usable = kind != TrainingKind.EXPERT || department.expertEligible(specialist).contains(option);
            Table pick = new Table();
            pick.setBackground(active
                ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.1f), Theme.GOLD, Theme.BORDER_EMPHASIS)
                : usable ? ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN)
                : ui.solid(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.08f)));
            Color tone = usable ? Theme.NAVY : Theme.INK_MUTED;
            Image glyph = new Image(ui.icon(department.icon(option)), Scaling.fit);
            glyph.setColor(tone);
            pick.add(glyph).height(18f).width(18f * Math.min(1f, ui.iconAspect(department.icon(option)))).padBottom(4f)
                .row();
            pick.add(ui.label(department.shortLabel(option), Theme.TextStyle.CAPTION, tone)).row();
            {
                // The current value, so the player sees what training would build on; a gold star on an Expert
                // skill (astronauts and controllers only).
                boolean expert = department.hasExperts() && department.isExpert(specialist, option);
                Table value = new Table();
                value.add(ui.label(String.valueOf(Math.round(department.skill(specialist, option))),
                    Theme.TextStyle.BODY_STRONG, expert ? Theme.GOLD_TEXT : tone));
                if (expert) {
                    Image star = new Image(ui.icon("prestige"), Scaling.fit);
                    star.setColor(Theme.GOLD_TEXT);
                    value.add(star).size(10f).padLeft(3f);
                }
                pick.add(value);
            }
            Tips.Tip tip = Tips.skill(department.label(option), option);
            ui.tooltip(pick, tip.title(), tip.text());
            if (!usable) {
                return pick;
            }
            pick.setTouchable(Touchable.enabled);
            pick.addListener(ChromeScreen.clicked(() -> {
                choose(kind, option);
                show();
            }));
            return pick;
        }

        private void confirm() {
            if (skill == null) {
                return;
            }
            double level = kind == TrainingKind.PERMANENT ? target : 0;
            String text = "Started training — " + specialist.getName() + " ("
                + kind.name().charAt(0) + kind.name().substring(1).toLowerCase(Locale.ROOT) + ", "
                + department.label(skill) + ").";
            PlayerAction action = kind == TrainingKind.EXPERT
                ? new StartExpertTrainingAction(department.group(), specialist.getId(), skill)
                : new StartTrainingAction(department.group(), specialist.getId(), kind, skill, level);
            host.submit(action,
                result -> {
                    host.closeDialog();
                    host.toast(text, Theme.NoticeKind.SUCCESS);
                });
        }
    }

    // ------------------------------------------------------------------ confirmations

    /**
     * Asks to pull an engineer out of training (component 5).
     *
     * @param specialist the engineer
     */
    void confirmPullOut(Personnel specialist) {
        Table card = host.dialogCard("Confirm", "Pull " + specialist.getName() + " out of training?");
        boolean expert = specialist.getTrainingKind() == TrainingKind.EXPERT;
        card.add(ui.paragraph(expert ? "Expert training gives nothing for the seasons so far."
            : roundCredit(specialist), Theme.TextStyle.BODY,
            Theme.INK_MUTED)).growX().padBottom(Theme.SPACE_4).row();
        card.add(confirmButtons("Pull Out", Theme.ButtonKind.PRIMARY, () -> host.submit(
            new PullOutOfTrainingAction(department.group(), specialist.getId()), result -> {
                host.closeDialog();
                host.toast("Pulled " + specialist.getName() + " out of training.", Theme.NoticeKind.SUCCESS);
            }))).right().fill(false, false);
        host.showDialog(card);
    }

    /**
     * The percentage of a fixed-length round's gain a pull-out now keeps
     * ({@link SkillGrowth#partialCreditShare}).
     *
     * @param person someone in a fixed-length training round
     * @return the share kept, in whole percent
     */
    static int keptPercent(Personnel person) {
        if (person.getTrainingSeasonsTotal() <= 0) {
            return 0;
        }
        return (int) Math.round(SkillGrowth.partialCreditShare(person.getTrainingSeasonsCompleted(),
            person.getTrainingSeasonsTotal()) * 100);
    }

    /**
     * What pulling someone out of a fixed-length round costs, in words: the share of the
     * round's gain they keep, and that the last season pays the most (user, 2026-11-05).
     *
     * @param person someone in a fixed-length training round
     * @return the sentence
     */
    static String roundCredit(Personnel person) {
        int kept = keptPercent(person);
        String last = Math.round(SkillGrowth.TRAINING_LAST_SEASON_SHARE * 100)
            + " % of the gain comes with the round's last season.";
        return kept == 0
            ? "They keep nothing of this round yet. " + last
            : "They keep " + kept + " % of this round's gain. " + last;
    }

    /**
     * The people an "All hands on deck" would pull out: everyone in training but basic training,
     * which can't be cut short (PullOutOfTrainingOutcome.BASIC_TRAINING).
     *
     * @param roster the department's roster
     * @return those who can be pulled out
     */
    static List<Personnel> pullable(List<? extends Personnel> roster) {
        return roster.stream().filter(person -> person.getAssignmentState() == AssignmentState.IN_TRAINING
            && person.getTrainingKind() != TrainingKind.BASIC && person.getTrainingKind() != TrainingKind.NONE)
            .map(Personnel.class::cast).toList();
    }

    /**
     * Adds the "All hands on deck" button (the user's request, 2026-11-01) to a title bar, when
     * anyone can be pulled out.
     *
     * @param titleActions the title bar
     * @param roster       the department's roster
     */
    void addAllHands(Table titleActions, List<? extends Personnel> roster) {
        List<Personnel> people = pullable(roster);
        if (!people.isEmpty()) {
            var button = host.onClick(ui.pageButton("All hands on deck · " + people.size()),
                () -> confirmAllHands(roster));
            ui.tooltip(button, "All hands on deck", "Pulls everyone out of training who can be. Basic training "
                + "can't be cut short.");
            titleActions.add(button).padRight(10f);
        }
    }

    /**
     * Asks to pull everyone out of training who can be, saying what it costs (component 5).
     *
     * @param roster the department's roster
     */
    void confirmAllHands(List<? extends Personnel> roster) {
        List<Personnel> people = pullable(roster);
        long experts = people.stream().filter(person -> person.getTrainingKind() == TrainingKind.EXPERT).count();
        long rounds = people.size() - experts;
        String noun = switch (department) {
            case ASTRONAUT -> "astronaut";
            case MISSION_CONTROL -> "controller";
            case SET -> "engineer";
        };
        String who = people.size() + " " + noun + (people.size() == 1 ? "" : "s");
        Table card = host.dialogCard("Confirm", "Pull " + who + " out of training?");
        StringBuilder body = new StringBuilder();
        if (rounds > 0) {
            var kept = people.stream().filter(person -> person.getTrainingKind() != TrainingKind.EXPERT)
                .mapToInt(PersonnelDialogs::keptPercent).summaryStatistics();
            String share = kept.getMin() == kept.getMax() ? kept.getMax() + " %"
                : kept.getMin() + " to " + kept.getMax() + " %";
            body.append(rounds == 1 ? "1 keeps " : rounds + " keep ").append(share)
                .append(" of the round in progress: ")
                .append(Math.round(SkillGrowth.TRAINING_LAST_SEASON_SHARE * 100))
                .append(" % of the gain comes with a round's last season.");
        }
        if (experts > 0) {
            body.append(rounds > 0 ? " " : "").append(experts).append(" in Expert training ")
                .append(experts == 1 ? "gets" : "get").append(" nothing for the seasons so far.");
        }
        if (roster.stream().anyMatch(person -> person.getAssignmentState() == AssignmentState.IN_TRAINING
            && person.getTrainingKind() == TrainingKind.BASIC)) {
            body.append(" Anyone in basic training stays in it.");
        }
        card.add(ui.paragraph(body.toString(), Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(confirmButtons("Pull Out All", Theme.ButtonKind.PRIMARY, () -> {
            host.closeDialog();
            pullOutNext(people, 0, who);
        })).right().fill(false, false);
        host.showDialog(card);
    }

    /**
     * Adds the "Keep Busy" button (the user's request, 2026-11-11) to a title bar, when anyone idle
     * has a skill below {@link KeepBusy#TARGET}.
     *
     * @param titleActions the title bar
     * @param roster       the department's roster
     */
    void addKeepBusy(Table titleActions, List<? extends Personnel> roster) {
        if (!KeepBusy.plan(department, roster).isEmpty()) {
            var button = host.onClick(ui.pageButton("Keep Busy"), () -> keepBusy(roster));
            ui.tooltip(button, "Keep Busy", "Puts everyone idle into permanent training to " + KeepBusy.TARGET
                + " in their highest skill below it, as far as their other skills allow.");
            titleActions.add(button).padRight(10f);
        }
    }

    /**
     * Starts permanent training for everyone idle, as the Keep Busy button does.
     *
     * @param roster the department's roster
     */
    void keepBusy(List<? extends Personnel> roster) {
        List<KeepBusy.Assignment> plan = KeepBusy.plan(department, roster);
        if (!plan.isEmpty()) {
            trainNext(plan, 0);
        }
    }

    /** One action per person, each sent when the last one is through; a refusal stops the rest. */
    private void trainNext(List<KeepBusy.Assignment> plan, int index) {
        KeepBusy.Assignment next = plan.get(index);
        host.submit(new StartTrainingAction(department.group(), next.person().getId(), TrainingKind.PERMANENT,
            next.skill(), KeepBusy.TARGET), result -> {
                if (index + 1 < plan.size()) {
                    trainNext(plan, index + 1);
                } else {
                    String noun = switch (department) {
                        case ASTRONAUT -> "astronaut";
                        case MISSION_CONTROL -> "controller";
                        case SET -> "engineer";
                    };
                    host.toast("Started permanent training for " + plan.size() + " " + noun
                        + (plan.size() == 1 ? "" : "s") + ".", Theme.NoticeKind.SUCCESS);
                }
            });
    }

    /** One action per person, each sent when the last one is through; a refusal stops the rest. */
    private void pullOutNext(List<Personnel> people, int index, String who) {
        host.submit(new PullOutOfTrainingAction(department.group(), people.get(index).getId()), result -> {
            if (index + 1 < people.size()) {
                pullOutNext(people, index + 1, who);
            } else {
                host.toast("Pulled " + who + " out of training.", Theme.NoticeKind.SUCCESS);
            }
        });
    }

    /**
     * Asks to dismiss an engineer (component 5).
     *
     * @param specialist the engineer
     */
    void confirmDismiss(Personnel specialist) {
        // Someone on a mission can't go; the server says ASSIGNED_TO_MISSION, so say it first.
        if (department != Department.SET && specialist.getAssignmentState() == AssignmentState.ASSIGNED) {
            host.toast(Refusals.text("ASSIGNED_TO_MISSION"), Theme.NoticeKind.INFO);
            return;
        }
        Table card = host.dialogCard("Confirm", "Dismiss " + specialist.getName() + "?");
        String body = specialist instanceof SetSpecialist engineer
            && specialist.getAssignmentState() == AssignmentState.ASSIGNED
            ? "This also frees their R&D slot on " + ContentNames.of(engineer.getRdTargetComponentId())
                + ". Dismissal is free, but final."
            : "Dismissal is free, but final.";
        card.add(ui.paragraph(body, Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(confirmButtons("Dismiss", Theme.ButtonKind.DESTRUCTIVE, () -> host.submit(
            new DismissAction(department.group(), specialist.getId()), result -> {
                host.closeDialog();
                host.toast("Dismissed " + specialist.getName() + ".", Theme.NoticeKind.SUCCESS);
            }))).right().fill(false, false);
        host.showDialog(card);
    }

    private Table confirmButtons(String label, Theme.ButtonKind kind, Runnable action) {
        Table buttons = new Table();
        buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).padRight(10f);
        buttons.add(button(label, kind, action));
        return buttons;
    }

    /**
     * Asks to raise one person ({@code ui-spec.md} &sect;4.23): to what they expect, or by a percentage. The
     * preview is the rule's own ({@link PayExpectations#preview}).
     *
     * @param person  the person
     * @param percent the raise in percent, or 0 for their expectation
     */
    void confirmRaise(Personnel person, int percent) {
        String surname = person.getName().substring(person.getName().lastIndexOf(' ') + 1);
        PayExpectations.Preview preview = PayExpectations.preview(person, percent);
        Table card = host.dialogCard(department.place() + " · raise", percent == 0
            ? "Meet " + person.getName() + "\u2019s expectation" : "Raise " + person.getName() + " by " + percent + " %");
        card.add(ui.paragraph(percent == 0
            ? "The salary rises to the " + PayTexts.money(preview.salary()) + " " + surname + " expects, for good. "
                + "One raise a person each season."
            : "The salary rises " + percent + " %, for good. One raise a person each season.",
            Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        Table box = new Table();
        box.setBackground(ui.cardAccent(Theme.NAVY));
        box.pad(Theme.SPACE_4, Theme.SPACE_4 + Theme.BORDER_ACCENT, Theme.SPACE_4, Theme.SPACE_4);
        box.defaults().left().expandX().top();
        box.add(previewCell("New salary", PayTexts.money(preview.salary()) + " a season",
            "from " + PayTexts.money(person.getSalary()), Theme.INK));
        box.add(previewCell("Extra per season", "+ " + PayTexts.money(preview.extra()),
            "a raise of " + preview.percent() + " %", Theme.INK)).row();
        int ceiling = PayExpectations.moraleCeiling(withSalary(person, preview.salary()));
        box.add(previewCell("Morale", person.getMorale() + " \u2192 " + preview.moraleAfter(),
            ceiling >= 100 ? "+1 a percent; no ceiling once paid as expected" : "+1 a percent; pay then holds it to "
                + ceiling, RosterCells.moraleColor(preview.moraleAfter()))).padTop(Theme.SPACE_3);
        box.add(previewCell("Loyalty", person.getLoyalty() + " \u2192 " + preview.loyaltyAfter(), "half as much",
            Theme.NAVY)).padTop(Theme.SPACE_3);
        card.add(box).padBottom(Theme.SPACE_4).row();
        card.add(confirmButtons("OK", Theme.ButtonKind.PRIMARY, () -> host.submit(
            new RaiseSalaryAction(person.getId(), percent), result -> {
                host.closeDialog();
                host.toast("Raised " + person.getName() + " to " + PayTexts.money(preview.salary()) + ".",
                    Theme.NoticeKind.SUCCESS);
            }))).right().fill(false, false);
        host.showDialog(card, RAISE_DIALOG_WIDTH);
    }

    /** The confirm's width, from the mockup. */
    private static final float RAISE_DIALOG_WIDTH = 480f;

    private Table previewCell(String label, String value, String caption, com.badlogic.gdx.graphics.Color color) {
        Table cell = new Table();
        cell.left();
        cell.defaults().left();
        cell.add(ui.label(label.toUpperCase(Locale.ROOT), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        cell.add(ui.label(value, Theme.TextStyle.HEADING_SM, color)).row();
        cell.add(ui.label(caption, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        return cell;
    }

    /** A stand-in at another salary, for the ceiling a raise would leave. */
    private static Personnel withSalary(Personnel person, double salary) {
        de.mkoehler.launchwindow.model.SetSpecialist copy = new de.mkoehler.launchwindow.model.SetSpecialist();
        copy.setSalary(salary);
        copy.setExpectedSalary(person.expectedSalary());
        return copy;
    }

    // ------------------------------------------------------------------ manage department

    /**
     * Opens the Manage department dialog (component 13): a one-off change to every salary in the
     * department by -50 to +50 percent, previewing its salary and average morale afterwards.
     *
     * @param percent the starting percentage
     */
    void openManageDepartment(int percent) {
        // Opens on "Bring everyone up" when anyone is paid below what they expect (ui-spec.md section 4.23).
        openManageDepartment(percent, !PayExpectations.belowExpectation(host.session.state(), department.group())
            .isEmpty());
    }

    /**
     * Opens the Manage department dialog in one of its two modes.
     *
     * @param percent the starting percentage for the same raise
     * @param bring   whether it opens on "Bring everyone up to expectations"
     */
    void openManageDepartment(int percent, boolean bring) {
        List<? extends Personnel> roster = department.roster(host.session.state());
        if (roster.isEmpty()) {
            host.toast("There's nobody in the department yet.", Theme.NoticeKind.INFO);
            return;
        }
        if (DepartmentSalaries.adjustedThisSeason(host.session.state(), department.group())) {
            host.toast(Refusals.text("ALREADY_ADJUSTED"), Theme.NoticeKind.INFO);
            return;
        }
        Table card = host.dialogCard(department.place(), "Manage department");
        card.add(segmented(new String[] {"Same raise for everyone", "Bring everyone up to expectations"},
            bring ? 1 : 0, pick -> openManageDepartment(percent, pick == 1))).height(32f).padBottom(Theme.SPACE_4)
            .row();
        if (bring) {
            bringEveryoneUp(card, roster);
            return;
        }
        card.add(ui.paragraph("A one-off adjustment to every " + switch (department) {
            case SET -> "SET salary";
            case ASTRONAUT -> "astronaut's salary";
            case MISSION_CONTROL -> "flight controller's salary";
        } + ", once per season. Morale moves with it — up for a raise, "
            + "down three times as much for a cut.", Theme.TextStyle.BODY, Theme.INK_MUTED)).padBottom(Theme.SPACE_4)
            .row();
        Table head = new Table();
        head.add(ui.label("SALARY CHANGE", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX().left();
        Label value = ui.label(percentLabel(percent), Theme.TextStyle.BODY_SM, Theme.INK);
        head.add(value).right();
        card.add(head).padBottom(Theme.SPACE_2).row();
        Slider slider = new Slider(DepartmentSalaries.MIN_PERCENT, DepartmentSalaries.MAX_PERCENT, 1f, false,
            ui.slider());
        slider.setValue(percent);
        card.add(slider).padBottom(Theme.SPACE_4).row();

        Table preview = new Table();
        preview.setBackground(ui.cardAccent(Theme.NAVY));
        preview.pad(Theme.SPACE_4, Theme.SPACE_4 + Theme.BORDER_ACCENT, Theme.SPACE_4, Theme.SPACE_4);
        Label salary = ui.label("", Theme.TextStyle.HEADING_SM, Theme.INK);
        Label morale = ui.label("", Theme.TextStyle.HEADING_SM, Theme.INK);
        Table left = new Table();
        left.left();
        left.defaults().left();
        left.add(ui.label("PROJECTED SALARY / SEASON", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        left.add(salary);
        Table right = new Table();
        right.left();
        right.defaults().left();
        right.add(ui.label("PROJECTED AVERAGE MORALE", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        right.add(morale);
        preview.add(left).expandX().left();
        preview.add(right).expandX().left();
        card.add(preview).padBottom(Theme.SPACE_3).row();
        card.add(onceLine()).padBottom(Theme.SPACE_4).row();

        Table buttons = new Table();
        buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).padRight(10f);
        TextButton ok = button("OK", Theme.ButtonKind.PRIMARY, () -> {
            int chosen = Math.round(slider.getValue());
            host.submit(new AdjustSalariesAction(department.group(), chosen), result -> {
                host.closeDialog();
                host.toast("Adjusted " + switch (department) {
                    case SET -> "SET";
                    case ASTRONAUT -> "astronaut";
                    case MISSION_CONTROL -> "flight controller";
                } + " salaries by "
                    + percentLabel(chosen) + ".", Theme.NoticeKind.SUCCESS);
            });
        });
        buttons.add(ok);
        card.add(buttons).right().fill(false, false);

        Runnable update = () -> {
            int chosen = Math.round(slider.getValue());
            value.setText(percentLabel(chosen));
            salary.setText(String.format(Locale.ROOT, "$ %,.1f", DepartmentSalaries.projectedSalary(roster, chosen)));
            morale.setText(String.valueOf(DepartmentSalaries.projectedAverageMorale(roster, chosen)));
            ok.setDisabled(chosen == 0);
        };
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                update.run();
            }
        });
        update.run();
        host.showDialog(card, MANAGE_DIALOG_WIDTH);
    }

    /** Manage department's second mode: everyone paid below what they expect, brought up to it. */
    private void bringEveryoneUp(Table card, List<? extends Personnel> roster) {
        card.add(ui.paragraph("Raises everyone paid below what they expect to exactly that, for good. Each one's morale "
            + "rises a point per percent, loyalty half as much.", Theme.TextStyle.BODY, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_4).row();
        List<Personnel> under = PayExpectations.belowExpectation(host.session.state(), department.group());
        if (under.isEmpty()) {
            Table quiet = new Table();
            quiet.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            quiet.pad(Theme.SPACE_4);
            quiet.defaults().left().growX();
            quiet.add(ui.label("Nobody is underpaid.", Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            quiet.add(ui.paragraph("Everyone in the " + department.place() + " is paid what they expect. Expectations "
                + "rise at the next funding review and with good work.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
                .padTop(2f);
            card.add(quiet).padBottom(Theme.SPACE_3).row();
        } else {
            double extra = 0;
            int moraleAfter = 0;
            java.util.Map<Personnel, PayExpectations.Preview> previews = new java.util.LinkedHashMap<>();
            for (Personnel person : under) {
                PayExpectations.Preview preview = PayExpectations.preview(person, 0);
                previews.put(person, preview);
                extra += preview.extra();
            }
            for (Personnel person : roster) {
                PayExpectations.Preview preview = previews.get(person);
                moraleAfter += preview == null ? person.getMorale() : preview.moraleAfter();
            }
            int moraleNow = (int) Math.round(roster.stream().mapToInt(Personnel::getMorale).average().orElse(0));
            Table summary = new Table();
            summary.setBackground(ui.cardAccent(Theme.NAVY));
            summary.pad(Theme.SPACE_3, Theme.SPACE_4 + Theme.BORDER_ACCENT, Theme.SPACE_3, Theme.SPACE_4);
            summary.add(ui.label(under.size() + " of " + roster.size() + " underpaid  ·  + " + PayTexts.money(extra)
                + " a season  ·  average morale " + moraleNow + " \u2192 " + Math.round((double) moraleAfter
                / roster.size()), Theme.TextStyle.BODY_STRONG, Theme.INK)).left().expandX();
            card.add(summary).padBottom(Theme.SPACE_3).row();
            Table list = new Table();
            list.top().left();
            list.defaults().left().padBottom(6f);
            list.add(ui.label("UNDERPAID", Theme.TextStyle.LABEL, Theme.INK_MUTED)).expandX();
            list.add(ui.label("PAID \u2192 EXPECTS", Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(150f);
            list.add(ui.label("MORALE \u2192 AFTER", Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(130f).row();
            for (Map.Entry<Personnel, PayExpectations.Preview> entry : previews.entrySet()) {
                Personnel person = entry.getKey();
                PayExpectations.Preview preview = entry.getValue();
                Table who = new Table();
                who.left();
                who.add(RosterCells.avatar(ui, person)).size(40f).padRight(Theme.SPACE_2);
                Table name = new Table();
                name.left();
                name.defaults().left();
                name.add(ui.label(person.getName(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
                name.add(ui.label(PayTexts.under(person) + " % under", Theme.TextStyle.CAPTION, Theme.GOLD_TEXT));
                who.add(name);
                list.add(who).expandX();
                list.add(ui.label(PayTexts.money(person.getSalary()) + " \u2192 " + PayTexts.money(preview.salary()),
                    Theme.TextStyle.BODY_SM, Theme.INK)).width(150f);
                Table morale = new Table();
                morale.left();
                morale.add(ui.label(person.getMorale() + " \u2192 ", Theme.TextStyle.BODY_SM, Theme.INK));
                morale.add(ui.label(String.valueOf(preview.moraleAfter()), Theme.TextStyle.BODY_STRONG,
                    RosterCells.moraleColor(preview.moraleAfter())));
                list.add(morale).width(130f).row();
            }
            ScrollPane scroll = new ScrollPane(list, new ScrollPane.ScrollPaneStyle());
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            card.add(scroll).height(Math.min(7, under.size()) * 46f + 24f).padBottom(Theme.SPACE_3).row();
        }
        card.add(onceLine()).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).padRight(10f);
        TextButton ok = button("OK", Theme.ButtonKind.PRIMARY, () -> host.submit(
            new MeetExpectationsAction(department.group()), result -> {
                host.closeDialog();
                host.toast("Brought " + under.size() + (under.size() == 1 ? " person" : " people")
                    + " up to what they expect.", Theme.NoticeKind.SUCCESS);
            }));
        ok.setDisabled(under.isEmpty());
        buttons.add(ok);
        card.add(buttons).right().fill(false, false);
        host.showDialog(card, MANAGE_DIALOG_WIDTH);
    }

    /** Manage department's width with its list of the underpaid. */
    private static final float MANAGE_DIALOG_WIDTH = 640f;

    private Label onceLine() {
        return ui.label("Either way, once per season: the department's one change.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED);
    }

    /** A segmented choice, as Settings' Sound. */
    private Table segmented(String[] names, int selected, java.util.function.IntConsumer pick) {
        Table segments = new Table();
        segments.setBackground(ui.rect(new com.badlogic.gdx.graphics.Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER,
            Theme.BORDER_THIN));
        segments.pad(Theme.BORDER_THIN);
        for (int i = 0; i < names.length; i++) {
            int index = i;
            boolean on = i == selected;
            Table segment = new Table();
            segment.setBackground(ui.solid(on ? Theme.NAVY : Theme.SURFACE_CARD));
            segment.add(ui.label(names[i].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                on ? Theme.ON_FILL : Theme.NAVY));
            if (!on) {
                segment.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
                segment.addListener(SessionScreen.clicked(() -> pick.accept(index)));
            }
            segments.add(segment).expand().fill().uniformX();
        }
        return segments;
    }

    private static String percentLabel(int percent) {
        return (percent > 0 ? "+" : "") + percent + "%";
    }
}
