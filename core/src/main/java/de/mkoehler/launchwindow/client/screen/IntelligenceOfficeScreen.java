package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.actions.CancelIntelOperationAction;
import de.mkoehler.launchwindow.actions.MakeOfferAction;
import de.mkoehler.launchwindow.actions.QueueIntelOperationAction;
import de.mkoehler.launchwindow.actions.SetIntelligenceTargetAction;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.ReliabilityBar;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.TraitChip;
import de.mkoehler.launchwindow.client.view.IntelTexts;
import de.mkoehler.launchwindow.client.view.PoachTexts;
import de.mkoehler.launchwindow.client.view.Museum;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Dossier;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelCatch;
import de.mkoehler.launchwindow.model.IntelFact;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.IntelligenceState;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.model.Tidbit;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.rules.IntelligenceFound;
import de.mkoehler.launchwindow.rules.Intelligence;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.BuildingUpgrades;
import de.mkoehler.launchwindow.rules.Poaching;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The Intelligence Office ({@code ui-spec.md} &sect;4.15, Intelligence Office canvas, 2026-10-07):
 * the service's level and the target it moves toward, this season's actions, what each level
 * brings and counter-intelligence on the overview; queuing operations against rivals; and a folder
 * per rival with everything learned so far. The background check itself is on the hire screens.
 *
 * @author Mario Koehler
 */
public final class IntelligenceOfficeScreen extends ChromeScreen {

    /** The office's tabs. */
    public enum Tab {
        /** The level, actions, ladder and counter-intelligence. */
        OVERVIEW("Overview"),
        /** Queuing operations. */
        OPERATIONS("Operations"),
        /** A folder per rival. */
        FOLDERS("Folders");

        private final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final float COLUMN_GAP = Theme.SPACE_4;
    private static final String HISTORY_NOTE = "In 1963 a reconnaissance satellite caught the first signs of a "
        + "huge new building site at Baikonur. In December 1967 the President's Daily Brief reported that a US "
        + "satellite had, for the first time, photographed a giant rocket on one of the new pads: the N1, the "
        + "Soviet Moon rocket.";

    private Tab tab;
    private boolean rivalsFailed;
    private IntelOperation.Kind kind = IntelOperation.Kind.ASSESSMENT;
    private String rivalKey;
    private IntelArea area = IntelArea.RND;
    private String group = PersonnelGroup.SET.name();
    private String folderKey;
    private int offerRaise = Poaching.RAISE_PERCENTS.getFirst();

    /**
     * Builds the office on a tab, fetching the rival agencies.
     *
     * @param game the game, in a session
     * @param tab  the tab to show
     */
    public IntelligenceOfficeScreen(LaunchWindowGame game, Tab tab) {
        super(game, List.of(Crumb.SPACE_COMPLEX, new Crumb("Intelligence Office", null)), "Intelligence Office",
            "Overview");
        this.tab = tab;
        rebuild();
        fetchRivals();
    }

    private void fetchRivals() {
        game.gateway().fetchRivals(session.credentials(), result -> {
            if (!isShowing()) {
                return;
            }
            rivalsFailed = !result.ok();
            if (result.ok()) {
                session.keepRivals(result.rivals());
            }
            rebuild();
        });
    }

    /**
     * Switches tabs.
     *
     * @param tab the tab to show
     */
    public void select(Tab tab) {
        this.tab = tab;
        rebuild();
    }

    /**
     * Opens a rival's folder.
     *
     * @param key the rival's key
     */
    public void openFolder(String key) {
        folderKey = key;
        select(Tab.FOLDERS);
    }

    /**
     * Picks a rival for a new operation, as a click on its row does.
     *
     * @param key the rival's key
     */
    public void pickRival(String key) {
        rivalKey = key;
        rebuild();
    }

    /**
     * Picks an assessment's area, as the area control does.
     *
     * @param area the area
     */
    public void pickArea(IntelArea area) {
        this.area = area;
        rebuild();
    }

    @Override
    protected void goIntelligenceOffice() {
        select(Tab.OVERVIEW);
    }

    private IntelligenceState intel() {
        return session.state().getIntelligence();
    }

    @Override
    protected void rebuild() {
        titleActions.clearChildren();
        if (intel().getLevel() >= Intelligence.PERSONNEL_FILE_LEVEL) {
            titleActions.add(ui.label("Offer: " + Poaching.queuedOffers(session.state()).size() + " of "
                + Poaching.OFFERS_PER_SEASON + " used", Theme.TextStyle.CAPTION, Theme.ON_PAGE_STRONG))
                .padRight(Theme.SPACE_3);
        }
        for (Tab each : Tab.values()) {
            titleActions.add(tabButton(each)).padLeft(Theme.SPACE_2);
        }
        content.clearChildren();
        content.top();
        content.defaults().growX();
        int level = intel().getLevel();
        setTitles("Intelligence Office" + (level > 0 ? " · " + IntelTexts.levelName(level) : ""), tab.label);
        Table body = switch (tab) {
            case OVERVIEW -> overview();
            case OPERATIONS -> operations();
            case FOLDERS -> folders();
        };
        content.add(scrollPane(body)).expand().fill();
    }

    private Table tabButton(Tab each) {
        boolean active = each == tab;
        boolean locked = each == Tab.OPERATIONS && intel().getLevel() < Intelligence.ASSESSMENT_LEVEL;
        Table button = new Table();
        button.setBackground(ui.navTab(active));
        button.pad(Theme.SPACE_2, Theme.SPACE_4, Theme.SPACE_2, Theme.SPACE_4);
        Color color = active ? Theme.NAVY : Theme.ON_PAGE_STRONG;
        if (locked) {
            Image lock = new Image(ui.icon("lock"), Scaling.fit);
            lock.setColor(color);
            button.add(lock).size(11f).padRight(5f);
        }
        button.add(ui.label(each.label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, color));
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(() -> select(each)));
        if (locked) {
            ui.tooltip(button, "Operations", "Agency assessments need the Intelligence Office at level "
                + Intelligence.ASSESSMENT_LEVEL + ". You can still look at the folders and plan here.");
        }
        return button;
    }

    // ---- Overview

    private Table overview() {
        Table columns = new Table();
        columns.top();
        Table left = column();
        left.add(serviceCard()).padBottom(COLUMN_GAP).row();
        if (intel().getLevel() > 0) {
            left.add(actionsCard()).row();
        }
        Table middle = column();
        middle.add(ladderCard()).row();
        Table right = column();
        right.add(counterCard()).padBottom(COLUMN_GAP).row();
        right.add(foldersCard()).padBottom(COLUMN_GAP).row();
        right.add(historyNote()).row();
        columns.add(left).width(520f).top().padRight(COLUMN_GAP);
        columns.add(middle).expandX().fillX().top().padRight(COLUMN_GAP);
        columns.add(right).width(420f).top();
        return columns;
    }

    private static Table column() {
        Table column = new Table();
        column.top();
        column.defaults().growX();
        return column;
    }

    private Label sectionTitle(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
    }

    private Table serviceCard() {
        IntelligenceState state = intel();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        boolean none = state.getLevel() == 0 && state.getTargetLevel() == 0;
        card.add(sectionTitle("Service level")).padBottom(Theme.SPACE_1).row();
        Table head = new Table();
        head.add(ui.label(none ? "No intelligence service yet" : "Level " + state.getLevel() + " of "
            + Intelligence.MAX_LEVEL + " · " + IntelTexts.levelName(state.getLevel()), Theme.TextStyle.HEADING_SM,
            Theme.INK)).expandX().left();
        String reaches = IntelTexts.reaches(session.state());
        if (reaches != null) {
            head.add(ui.activeBadge(reaches)).right();
        }
        card.add(head).padBottom(Theme.SPACE_3).row();
        if (none) {
            card.add(ui.paragraph("An intelligence service learns what others would rather you didn't know. It never "
                + "breaks anything: it only buys information.", Theme.TextStyle.BODY, Theme.INK))
                .padBottom(Theme.SPACE_3).row();
            String[][] steps = {
                {"Your own people.", "Check a candidate's background before hiring, and sometimes spot a trait for "
                    + "free."},
                {"Rival agencies.", "Assess a rival's R&D, plans or staff, and pick up tidbits along the way."},
                {"Rivals' people.", "Open a file on one of their engineers, astronauts or controllers."}};
            for (int i = 0; i < steps.length; i++) {
                Table step = new Table();
                step.add(stepNumber(i + 1)).top().padRight(Theme.SPACE_2);
                step.add(ui.paragraph(steps[i][0] + " " + steps[i][1], Theme.TextStyle.BODY_SM, Theme.INK)).expandX()
                    .fillX();
                card.add(step).padBottom(Theme.SPACE_2).row();
            }
            card.add(ui.paragraph("A service also makes your agency harder to spy on. Without one, a rival's operation "
                + "against you succeeds " + Intelligence.successPercent(0) + " % of the time.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        }
        card.add(sectionTitle("Target level")).padBottom(Theme.SPACE_2).row();
        card.add(stepper()).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(none ? "Raise the target to start. Level 1 costs " + IntelTexts.perSeason(
            Intelligence.cost(1)) + " and arrives next season; each further level takes one more season."
            : "Changes take effect next season and move one level a season, up or down.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_3).row();
        if (!none) {
            Table costs = new Table();
            costs.defaults().left().top().padBottom(Theme.SPACE_1);
            kv(costs, "Cost now", IntelTexts.perSeason(Intelligence.cost(state.getLevel())));
            kv(costs, "At the target", IntelTexts.perSeason(Intelligence.cost(state.getTargetLevel())));
            String way = IntelTexts.onTheWay(session.state());
            if (way != null) {
                kv(costs, "On the way", way);
            }
            card.add(costs).row();
        }
        return card;
    }

    private void kv(Table table, String key, String value) {
        table.add(sectionTitle(key)).width(120f).padRight(Theme.SPACE_3);
        table.add(ui.paragraph(value, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX().row();
    }

    private Table stepNumber(int number) {
        Table circle = new Table();
        circle.setBackground(ui.circle(Theme.NAVY, Theme.NAVY, 0f, 20));
        circle.add(ui.label(String.valueOf(number), Theme.TextStyle.LABEL, Theme.ON_FILL));
        Table holder = new Table();
        holder.add(circle).size(20f);
        return holder;
    }

    private Table stepper() {
        IntelligenceState state = intel();
        Table stepper = new Table();
        stepper.add(stepButton("−", state.getTargetLevel() > 0, state.getTargetLevel() - 1)).size(36f)
            .padRight(Theme.SPACE_1);
        for (int level = 0; level <= Intelligence.MAX_LEVEL; level++) {
            stepper.add(segment(level)).expandX().fillX().height(44f).padRight(Theme.SPACE_1);
        }
        stepper.add(stepButton("+", state.getTargetLevel() < Intelligence.MAX_LEVEL, state.getTargetLevel() + 1))
            .size(36f);
        return stepper;
    }

    private Table stepButton(String text, boolean enabled, int target) {
        Table button = new Table();
        button.setBackground(ui.rect(Theme.SURFACE_CARD, enabled ? Theme.NAVY : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        button.add(ui.label(text, Theme.TextStyle.HEADING_SM, enabled ? Theme.NAVY : Theme.NAVY_BORDER));
        if (enabled) {
            button.setTouchable(Touchable.enabled);
            button.addListener(clicked(() -> setTarget(target)));
        }
        return button;
    }

    private Table segment(int level) {
        IntelligenceState state = intel();
        boolean current = level == state.getLevel();
        boolean target = level == state.getTargetLevel();
        boolean below = level < state.getLevel();
        Table segment = new Table();
        Color fill = current ? Theme.NAVY : below ? new Color(Theme.NAVY).lerp(Theme.SURFACE_CARD, 0.75f)
            : Theme.SURFACE_CARD;
        segment.setBackground(ui.rect(fill, target ? Theme.GOLD : Theme.NAVY_BORDER,
            target ? Theme.BORDER_ACCENT - 1f : Theme.BORDER_THIN));
        Color ink = current ? Theme.ON_FILL : Theme.INK;
        segment.add(ui.label(String.valueOf(level), Theme.TextStyle.HEADING_SM, ink)).row();
        String tag = current && target ? "Now · target" : current ? "Now" : target ? "Target" : " ";
        segment.add(ui.label(tag, Theme.TextStyle.CAPTION, current ? Theme.ON_FILL : Theme.INK_MUTED));
        segment.setTouchable(Touchable.enabled);
        segment.addListener(clicked(() -> setTarget(level)));
        ui.tooltip(segment, "Level " + level + " · " + IntelTexts.levelName(level),
            IntelTexts.perSeason(Intelligence.cost(level)) + ". " + IntelTexts.unlocks(level) + ".");
        return segment;
    }

    /**
     * Sets the target level, as the stepper does.
     *
     * @param target the level
     */
    public void setTarget(int target) {
        if (target == intel().getTargetLevel()) {
            return;
        }
        submit(new SetIntelligenceTargetAction(target), result -> toast(target == intel().getLevel()
            ? "The service stays at level " + target + "." : "Target set: level " + target + ". It moves one level a "
            + "season from the next resolution.", Theme.NoticeKind.SUCCESS));
    }

    private Table actionsCard() {
        IntelligenceState state = intel();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(sectionTitle("This season's actions")).expandX().left();
        head.add(ui.label(state.getActionsUsed() + " of " + state.getLevel() + " used", Theme.TextStyle.BODY_STRONG,
            Theme.INK));
        card.add(head).padBottom(Theme.SPACE_3).row();
        List<IntelOperation> queued = state.getQueued();
        int checks = Math.max(0, state.getActionsUsed() - queued.size());
        int slot = 1;
        for (IntelOperation operation : queued) {
            card.add(actionLine(slot++, IntelTexts.operation(operation.kind(), operation.area(), operation.group())
                + " · " + agencyName(operation.targetId()), "Queued. The answer comes in the next turn report.",
                true)).padBottom(Theme.SPACE_2).row();
        }
        for (int i = 0; i < checks; i++) {
            card.add(actionLine(slot++, "Background check", "Answered at once on a hire screen.", true))
                .padBottom(Theme.SPACE_2).row();
        }
        while (slot <= state.getLevel()) {
            String could = state.getLevel() >= Intelligence.ASSESSMENT_LEVEL
                ? "A background check on the hire screens, or an operation." : "A background check on the hire screens.";
            card.add(actionLine(slot++, "Free", could, false)).padBottom(Theme.SPACE_2).row();
        }
        String more = state.getTargetLevel() > state.getLevel() ? " Level " + (state.getLevel() + 1) + " brings "
            + (state.getLevel() + 1 == 1 ? "one" : "another") + " from next season." : "";
        card.add(ui.paragraph("Unused actions don't carry over." + more, Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .row();
        return card;
    }

    private Table actionLine(int number, String title, String detail, boolean used) {
        Table line = new Table();
        line.add(stepNumber(number)).top().padRight(Theme.SPACE_3);
        Table text = new Table();
        text.defaults().left();
        text.add(ui.label(title, Theme.TextStyle.BODY_SM, used ? Theme.INK : Theme.INK_MUTED)).row();
        text.add(ui.paragraph(detail, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        line.add(text).expandX().fillX();
        return line;
    }

    private Table ladderCard() {
        IntelligenceState state = intel();
        Table card = ui.card();
        card.top();
        card.defaults().growX();
        Table head = new Table();
        head.add(sectionTitle("What each level brings")).expandX().left();
        head.add(ui.label("Each level keeps everything below it", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(head).padBottom(Theme.SPACE_3).row();
        for (int level = Intelligence.MAX_LEVEL; level >= 0; level--) {
            card.add(rung(level, state)).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table rung(int level, IntelligenceState state) {
        boolean current = level == state.getLevel();
        boolean target = level == state.getTargetLevel() && !current;
        boolean locked = level > state.getLevel();
        Table rung = new Table();
        rung.setBackground(ui.rect(current ? new Color(Theme.NAVY).lerp(Theme.SURFACE_CARD, 0.9f) : Theme.SURFACE_CARD,
            current ? Theme.NAVY : target ? Theme.GOLD : Theme.NAVY_BORDER, current || target ? Theme.BORDER_EMPHASIS
                : Theme.BORDER_THIN));
        rung.pad(Theme.SPACE_3);
        Table number = new Table();
        number.setBackground(ui.solid(current ? Theme.NAVY : locked ? Theme.SKILL_TRACK : Theme.NAVY_BORDER));
        number.add(ui.label(String.valueOf(level), Theme.TextStyle.HEADING_SM, current ? Theme.ON_FILL : Theme.INK));
        rung.add(number).size(36f).top().padRight(Theme.SPACE_3);
        Table body = new Table();
        body.defaults().left();
        Table title = new Table();
        title.add(ui.label(IntelTexts.levelName(level), Theme.TextStyle.HEADING_SM, locked ? Theme.INK_MUTED
            : Theme.INK)).padRight(Theme.SPACE_2);
        title.add(ui.label(IntelTexts.perSeason(Intelligence.cost(level)), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        title.add().expandX();
        if (current) {
            title.add(ui.badge(Theme.BadgeKind.SUCCESS, "Now"));
        } else if (target) {
            title.add(ui.activeBadge("Target · " + (Math.abs(level - state.getLevel()) == 1 ? "next season"
                : "in " + Math.abs(level - state.getLevel()) + " seasons")));
        }
        body.add(title).growX().padBottom(Theme.SPACE_2).row();
        Table grid = new Table();
        grid.defaults().left().top().padBottom(2f);
        rungLine(grid, "Actions", level == 0 ? "none" : level + " a season", false);
        rungLine(grid, "Unlocks", IntelTexts.unlocks(level), locked && level > 0);
        rungLine(grid, "Passive", IntelTexts.passive(level), false);
        rungLine(grid, "Counter-intel", IntelTexts.counter(level), false);
        body.add(grid).growX();
        rung.add(body).expandX().fillX();
        if (locked) {
            rung.getColor().a = 0.75f;
        }
        return rung;
    }

    private void rungLine(Table grid, String key, String value, boolean lock) {
        grid.add(sectionTitle(key)).width(100f).padRight(Theme.SPACE_2);
        Table cell = new Table();
        if (lock) {
            Image glyph = new Image(ui.icon("lock"), Scaling.fit);
            glyph.setColor(Theme.INK_MUTED);
            cell.add(glyph).size(11f).padRight(4f);
        }
        cell.add(ui.paragraph(value, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
        grid.add(cell).expandX().fillX().row();
    }

    private Table counterCard() {
        IntelligenceState state = intel();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(sectionTitle("Counter-intelligence")).padBottom(Theme.SPACE_2).row();
        Table pips = new Table();
        int next = state.getLevel() + Integer.signum(state.getTargetLevel() - state.getLevel());
        for (int i = 1; i <= Intelligence.MAX_LEVEL; i++) {
            boolean on = i <= state.getLevel();
            boolean coming = !on && i <= next;
            Table pip = new Table();
            pip.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, coming ? Theme.GOLD : Theme.NAVY,
                Theme.BORDER_EMPHASIS));
            pips.add(pip).size(14f).padRight(4f);
        }
        pips.add(ui.label(IntelTexts.counter(state.getLevel()), Theme.TextStyle.HEADING_SM, Theme.INK)).padLeft(
            Theme.SPACE_2).padRight(Theme.SPACE_2);
        if (next != state.getLevel()) {
            pips.add(ui.label((next > state.getLevel() ? "rises to " : "falls to ") + IntelTexts.counter(next)
                + " next season", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        card.add(pips).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(IntelTexts.counterEffect(state.getLevel()), Theme.TextStyle.BODY_SM, Theme.INK))
            .padBottom(Theme.SPACE_3).row();
        Table catches = new Table();
        catches.defaults().left().top().padBottom(Theme.SPACE_1);
        kv(catches, "Theirs caught", catchSummary(false));
        kv(catches, "Yours caught", catchSummary(true));
        card.add(catches).row();
        return card;
    }

    private String catchSummary(boolean ours) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, RivalFolder> folder : intel().getFolders().entrySet()) {
            for (IntelCatch caught : folder.getValue().getCatches()) {
                if (caught.ours() == ours) {
                    lines.add(folder.getValue().getAgencyName() + ", " + Museum.date(caught.turn())
                        + (ours ? "" : " (their Intelligence level: " + caught.level() + ")"));
                }
            }
        }
        return lines.isEmpty() ? "None so far" : lines.size() + " · " + String.join("; ", lines);
    }

    private Table foldersCard() {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(sectionTitle("Folders")).padBottom(Theme.SPACE_2).row();
        Map<String, String> rivals = rivalNames();
        if (rivals.isEmpty()) {
            card.add(ui.paragraph(session.rivals() == null && !rivalsFailed ? "Fetching the agencies…"
                : "No rival agencies yet.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (Map.Entry<String, String> rival : rivals.entrySet()) {
            RivalFolder folder = intel().getFolders().get(rival.getKey());
            Table line = new Table();
            line.add(ui.label(rival.getValue(), Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            int newest = newest(folder);
            line.add(ui.label(newest < 0 ? "Empty" : "Newest " + Museum.date(newest), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED));
            line.setTouchable(Touchable.enabled);
            line.addListener(clicked(() -> openFolder(rival.getKey())));
            card.add(line).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table historyNote() {
        Table card = new Table();
        card.setBackground(ui.historyNoteCard());
        card.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_4, Theme.SPACE_4);
        card.top();
        Table head = new Table();
        // The glyph is white so it can be tinted: navy, as on every other history note (component 15).
        Image glyph = new Image(ui.historyNoteIcon(), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        head.add(glyph).size(16f).padRight(Theme.SPACE_2);
        head.add(ui.label("HISTORY", Theme.TextStyle.LABEL, Theme.NAVY)).expandX().left();
        card.add(head).growX().padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph(HISTORY_NOTE, Theme.TextStyle.BODY_SM, Theme.INK)).growX();
        return card;
    }

    // ---- Operations

    private Table operations() {
        Table columns = new Table();
        columns.top();
        Table left = column();
        left.add(newOperationCard()).row();
        Table right = column();
        if (rivalKey != null && intel().getLevel() >= Intelligence.ASSESSMENT_LEVEL) {
            right.add(oddsCard()).padBottom(COLUMN_GAP).row();
        }
        right.add(queuedCard()).padBottom(COLUMN_GAP).row();
        right.add(resultsCard()).row();
        columns.add(left).expandX().fillX().top().padRight(COLUMN_GAP);
        columns.add(right).width(640f).top();
        return columns;
    }

    private Table newOperationCard() {
        IntelligenceState state = intel();
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label("New operation", Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        head.add(ui.label(Intelligence.actionsLeft(session.state()) + " of " + state.getLevel()
            + " actions left this season", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(head).padBottom(Theme.SPACE_4).row();
        if (state.getLevel() < Intelligence.ASSESSMENT_LEVEL) {
            card.add(ui.notice(Theme.NoticeKind.INFO, "Operations need level " + Intelligence.ASSESSMENT_LEVEL + ".",
                "Raise the target on the overview; the level climbs one step a season. Background checks need "
                    + "level " + Intelligence.CHECK_LEVEL + " and live on the hire screens.")).row();
            return card;
        }
        card.add(stepTitle(1, "Operation")).padBottom(Theme.SPACE_2).row();
        card.add(option(kind == IntelOperation.Kind.ASSESSMENT, false, "Agency assessment",
            "One area of one rival. Level " + Intelligence.ASSESSMENT_LEVEL + ".",
            () -> { kind = IntelOperation.Kind.ASSESSMENT; rebuild(); })).padBottom(Theme.SPACE_2).row();
        boolean filesOpen = state.getLevel() >= Intelligence.PERSONNEL_FILE_LEVEL;
        card.add(option(kind == IntelOperation.Kind.PERSONNEL_FILE, !filesOpen, "Personnel file",
            "One of a rival's people. Level " + Intelligence.PERSONNEL_FILE_LEVEL + (filesOpen ? "."
                : state.getTargetLevel() >= Intelligence.PERSONNEL_FILE_LEVEL && state.getLevel() + 1
                    == Intelligence.PERSONNEL_FILE_LEVEL ? ", from next season." : "."),
            () -> { kind = IntelOperation.Kind.PERSONNEL_FILE; rebuild(); })).padBottom(Theme.SPACE_4).row();

        card.add(stepTitle(2, "Rival agency")).padBottom(Theme.SPACE_2).row();
        List<RivalSummary> rivals = rivals();
        if (rivals.isEmpty()) {
            card.add(ui.paragraph(session.rivals() == null && !rivalsFailed ? "Fetching the agencies…"
                : rivalsFailed ? "Couldn't fetch the agencies. Open the office again to retry."
                : "No rival agencies yet.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        }
        for (RivalSummary rival : rivals) {
            card.add(rivalOption(rival)).padBottom(Theme.SPACE_2).row();
        }
        card.add().height(Theme.SPACE_2).row();
        if (kind == IntelOperation.Kind.ASSESSMENT) {
            card.add(stepTitle(3, "Area")).padBottom(Theme.SPACE_2).row();
            Table control = new Table();
            for (IntelArea each : IntelArea.values()) {
                control.add(segmentButton(IntelTexts.area(each).toUpperCase(Locale.ROOT), each == area,
                    () -> pickArea(each))).expandX().fillX().height(34f);
            }
            card.add(control).padBottom(Theme.SPACE_2).row();
            RivalFolder folder = rivalKey == null ? null : intel().getFolders().get(rivalKey);
            Dossier current = folder == null ? null : folder.getDossiers().get(area);
            card.add(ui.paragraph(IntelTexts.areaCovers(area) + (current == null ? "" : " The current dossier dates "
                + "from " + Museum.date(current.turn()) + "; a new one replaces it."), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).row();
        } else {
            card.add(stepTitle(3, "Group")).padBottom(Theme.SPACE_2).row();
            Table control = new Table();
            for (PersonnelGroup each : PersonnelGroup.values()) {
                control.add(segmentButton(IntelTexts.groupName(each.name()).toUpperCase(Locale.ROOT),
                    each.name().equals(group), () -> { group = each.name(); rebuild(); })).expandX().fillX()
                    .height(34f);
            }
            card.add(control).padBottom(Theme.SPACE_2).row();
            card.add(ui.paragraph("The file covers the group's most skilled person you don't have a file on yet.",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        }
        return card;
    }

    private Table stepTitle(int number, String text) {
        Table title = new Table();
        title.add(stepNumber(number)).padRight(Theme.SPACE_2);
        title.add(sectionTitle(text)).expandX().left();
        return title;
    }

    private Table option(boolean on, boolean locked, String title, String detail, Runnable action) {
        Table option = new Table();
        option.setBackground(ui.optionCard(on && !locked));
        option.pad(Theme.SPACE_3);
        Table radio = new Table();
        radio.setBackground(ui.circle(on && !locked ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, 1.5f, 14));
        option.add(radio).size(14f).top().padRight(Theme.SPACE_3);
        Table text = new Table();
        text.defaults().left();
        Table name = new Table();
        if (locked) {
            Image lock = new Image(ui.icon("lock"), Scaling.fit);
            lock.setColor(Theme.INK_MUTED);
            name.add(lock).size(11f).padRight(4f);
        }
        name.add(ui.label(title, Theme.TextStyle.BODY_STRONG, locked ? Theme.INK_MUTED : Theme.INK));
        text.add(name).row();
        text.add(ui.paragraph(detail, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        option.add(text).expandX().fillX();
        if (!locked) {
            option.setTouchable(Touchable.enabled);
            option.addListener(clicked(action));
        }
        return option;
    }

    private Table rivalOption(RivalSummary rival) {
        RivalFolder folder = intel().getFolders().get(rival.playerId());
        boolean on = rival.playerId().equals(rivalKey);
        Table option = new Table();
        option.setBackground(ui.optionCard(on));
        option.pad(Theme.SPACE_3);
        Table radio = new Table();
        radio.setBackground(ui.circle(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, 1.5f, 14));
        option.add(radio).size(14f).top().padRight(Theme.SPACE_3);
        Table text = new Table();
        text.defaults().left();
        text.add(ui.label(rival.agencyName(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        text.add(ui.label("Dir. " + rival.directorName() + " · " + Museum.archetype(rival.archetype()) + " · "
            + goals(rival) + " reached", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        text.add(ui.paragraph(IntelTexts.oddsSource(folder), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        option.add(text).expandX().fillX().padRight(Theme.SPACE_3);
        Table odds = new Table();
        odds.add(ui.label(IntelTexts.odds(folder), Theme.TextStyle.BODY_STRONG, Theme.INK)).right().row();
        odds.add(ui.label("success", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        option.add(odds).top();
        option.setTouchable(Touchable.enabled);
        option.addListener(clicked(() -> pickRival(rival.playerId())));
        return option;
    }

    private Table segmentButton(String text, boolean on, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        button.add(ui.label(text, Theme.TextStyle.LABEL, on ? Theme.ON_FILL : Theme.NAVY));
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(action));
        return button;
    }

    private Table oddsCard() {
        RivalFolder folder = intel().getFolders().get(rivalKey);
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(sectionTitle(agencyName(rivalKey) + " · " + (kind == IntelOperation.Kind.ASSESSMENT
            ? IntelTexts.area(area) : "personnel file")) ).padBottom(Theme.SPACE_3).row();
        Table numbers = new Table();
        numbers.defaults().top().left().expandX().fillX().uniformX();
        numbers.add(bigNumber(IntelTexts.odds(folder), "Succeeds", IntelTexts.oddsSource(folder)));
        String catchLine = folder == null || folder.getKnownLevel() < 0 ? "Depends on their counter-intelligence"
            : "A failure (" + (100 - Intelligence.successPercent(folder.getKnownLevel())) + " %) is caught "
                + Intelligence.catchPercent(folder.getKnownLevel()) + " % of the time";
        numbers.add(bigNumber(IntelTexts.caughtOdds(folder), "Caught", catchLine));
        numbers.add(bigNumber("Next turn", "Answer", "In the next turn report; the dossier is dated "
            + GameDate.ofTurn((int) session.latestResolvedTurn() + 1).label()));
        card.add(numbers).padBottom(Theme.SPACE_3).row();
        Table danger = new Table();
        danger.setBackground(ui.rect(new Color(Theme.DANGER).lerp(Theme.SURFACE_CARD, 0.92f), Theme.DANGER,
            Theme.BORDER_THIN));
        danger.pad(Theme.SPACE_3);
        danger.defaults().left().growX();
        danger.add(ui.label("If caught: −" + Intelligence.CAUGHT_PRESTIGE + " prestige", Theme.TextStyle.BODY_STRONG,
            Theme.DANGER)).row();
        danger.add(ui.paragraph(agencyName(rivalKey) + "'s turn report names " + session.state().getAgencyName()
            + ", and learns your Intelligence level. Yours says the operation was blown and what their "
            + "counter-intelligence was. A failure that isn't caught just learns nothing.", Theme.TextStyle.BODY_SM,
            Theme.INK));
        card.add(danger).padBottom(Theme.SPACE_3).row();
        Table foot = new Table();
        foot.add(ui.paragraph("Free until the turn resolves: cancel any time before.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).expandX().fillX().padRight(Theme.SPACE_3);
        boolean can = Intelligence.actionsLeft(session.state()) > 0
            && (kind == IntelOperation.Kind.ASSESSMENT || intel().getLevel() >= Intelligence.PERSONNEL_FILE_LEVEL);
        String label = Intelligence.actionsLeft(session.state()) == 1 ? "Queue · uses the last action" : "Queue";
        if (can) {
            foot.add(actionButton(label, Theme.ButtonKind.PRIMARY, this::queue));
        } else {
            foot.add(ui.label(Intelligence.actionsLeft(session.state()) == 0 ? "No action left this season"
                : "Needs level " + Intelligence.PERSONNEL_FILE_LEVEL, Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        card.add(foot).row();
        return card;
    }

    private Table bigNumber(String value, String label, String detail) {
        Table cell = new Table();
        cell.top().left();
        cell.defaults().left();
        cell.add(ui.label(value, Theme.TextStyle.HEADING_LG, Theme.NAVY)).row();
        cell.add(sectionTitle(label)).padBottom(2f).row();
        cell.add(ui.paragraph(detail, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(170f);
        return cell;
    }

    /**
     * Queues the operation picked on the Operations tab.
     */
    public void queue() {
        if (rivalKey == null) {
            return;
        }
        QueueIntelOperationAction action = kind == IntelOperation.Kind.ASSESSMENT
            ? QueueIntelOperationAction.assessment(rivalKey, area)
            : QueueIntelOperationAction.personnelFile(rivalKey, group, null);
        submit(action, result -> toast("Queued. The answer comes in the next turn report.",
            Theme.NoticeKind.SUCCESS));
    }

    private Table queuedCard() {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(sectionTitle("Queued this season")).expandX().left();
        head.add(ui.label("Settled at the next resolution", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        card.add(head).padBottom(Theme.SPACE_3).row();
        List<IntelOperation> queued = intel().getQueued();
        if (queued.isEmpty()) {
            card.add(ui.paragraph("Nothing queued.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(
                Theme.SPACE_2).row();
        }
        int number = 1;
        for (IntelOperation operation : queued) {
            if (operation.kind() == IntelOperation.Kind.OFFER) {
                card.add(queuedOffer(operation)).padBottom(Theme.SPACE_2).row();
                continue;
            }
            RivalFolder folder = intel().getFolders().get(operation.targetId());
            Table line = new Table();
            line.add(stepNumber(number++)).top().padRight(Theme.SPACE_3);
            Table text = new Table();
            text.defaults().left();
            text.add(ui.label(IntelTexts.operation(operation.kind(), operation.area(), operation.group()) + " · "
                + agencyName(operation.targetId()), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            text.add(ui.paragraph("Succeeds " + IntelTexts.odds(folder) + " · caught " + IntelTexts.caughtOdds(folder)
                + " · answer in the next turn report", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
            line.add(text).expandX().fillX().padRight(Theme.SPACE_3);
            line.add(actionButton("Cancel", Theme.ButtonKind.DESTRUCTIVE, () -> submit(
                new CancelIntelOperationAction(operation.id()), result -> toast("Cancelled; the action is free again.",
                    Theme.NoticeKind.SUCCESS))));
            card.add(line).padBottom(Theme.SPACE_2).row();
        }
        card.add(ui.paragraph("Cancelling gives the action back. Background checks don't queue: they answer at once "
            + "on the hire screens.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        return card;
    }

    /** A queued offer, set apart from the operations: its price, the room, and the answer as far as known. */
    private Table queuedOffer(IntelOperation offer) {
        PersonnelFile file = Poaching.file(session.state(), offer.targetId(), offer.personId());
        String agency = agencyName(offer.targetId());
        Table line = new Table();
        line.setBackground(ui.solid(new Color(Theme.GOLD.r, Theme.GOLD.g, Theme.GOLD.b, 0.14f)));
        line.pad(Theme.SPACE_2);
        Image glyph = new Image(ui.icon("funds"), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        line.add(glyph).size(16f).top().padRight(Theme.SPACE_3).padTop(2f);
        Table text = new Table();
        text.defaults().left();
        Table head = new Table();
        head.add(ui.label("Offer · " + (file == null ? "someone" : file.name()) + ", " + agency + " "
            + (file == null ? "" : IntelTexts.groupOne(file.group()).toLowerCase(Locale.ROOT)), Theme.TextStyle.BODY_STRONG,
            Theme.INK)).padRight(Theme.SPACE_2);
        head.add(queuedBadge(offer.raisePercent()));
        text.add(head).row();
        if (file != null) {
            text.add(ui.paragraph(PoachTexts.ifAccepted(file, offer.raisePercent()) + " " + room(file.group())
                + ". Their answer, as far as you can tell: " + PoachTexts.range(Poaching.answerRange(offer.raisePercent(),
                file)) + ", unless " + agency + " matches.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX().row();
        }
        text.add(ui.paragraph("The season's one offer. It uses no action and can't be caught.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).growX();
        line.add(text).expandX().fillX().padRight(Theme.SPACE_3);
        line.add(actionButton("Cancel", Theme.ButtonKind.DESTRUCTIVE, () -> cancelOffer(offer)));
        return line;
    }

    private void cancelOffer(IntelOperation offer) {
        submit(new CancelIntelOperationAction(offer.id()), result -> toast("Offer cancelled. You can make another "
            + "this season.", Theme.NoticeKind.SUCCESS));
    }

    private Table queuedBadge(int raisePercent) {
        Table badge = new Table();
        badge.setBackground(ui.solid(Theme.GOLD));
        badge.pad(2f, 6f, 2f, 6f);
        Image clock = new Image(ui.icon("clock"), Scaling.fit);
        clock.setColor(Theme.ON_GOLD);
        badge.add(clock).size(11f).padRight(4f);
        badge.add(ui.label("OFFER QUEUED: " + PoachTexts.raise(raisePercent), Theme.TextStyle.LABEL, Theme.ON_GOLD));
        return badge;
    }

    /** "SET Centre: 8 of 12", the room an offer needs in this agency. */
    private String room(String group) {
        PersonnelGroup which = PersonnelGroup.valueOf(group);
        int size = which == PersonnelGroup.SET ? session.state().getSetSpecialists().size()
            : session.state().getMissionControlOfficers().size();
        return PoachTexts.building(group) + ": " + size + " of " + capacity(which);
    }

    private int capacity(PersonnelGroup group) {
        return BuildingUpgrades.capacity(session.state(), group == PersonnelGroup.SET
            ? de.mkoehler.launchwindow.model.Building.SET_CENTER : de.mkoehler.launchwindow.model.Building.MISSION_CONTROL,
            session.catalog().buildings());
    }

    private Table resultsCard() {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        long turn = session.latestResolvedTurn();
        TurnReport report = turn > 0 ? session.report(turn) : null;
        card.add(sectionTitle("Last season's results" + (turn > 0 ? " · " + Museum.date((int) turn) : "")))
            .padBottom(Theme.SPACE_3).row();
        List<IntelligenceFound> found = report == null ? List.of() : report.events().stream()
            .filter(IntelligenceFound.class::isInstance).map(IntelligenceFound.class::cast).toList();
        if (found.isEmpty()) {
            card.add(ui.paragraph(report == null ? "Open the turn report to see last season's results here."
                : "Nothing came in last season.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (IntelligenceFound event : found) {
            String[] text = IntelTexts.finding(event.finding());
            int tone = IntelTexts.tone(event.finding());
            Table line = new Table();
            Image glyph = new Image(ui.icon(tone > 0 ? "check" : tone < 0 ? "close" : "binoculars"), Scaling.fit);
            glyph.setColor(tone > 0 ? Theme.SUCCESS : tone < 0 ? Theme.DANGER : Theme.INK_MUTED);
            line.add(glyph).size(14f).top().padRight(Theme.SPACE_3).padTop(2f);
            Table body = new Table();
            body.defaults().left();
            body.add(ui.label(text[0], Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            body.add(ui.paragraph(text[1], Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
            line.add(body).expandX().fillX();
            String key = event.finding().rivalId();
            if (key != null) {
                Label open = ui.label("Open folder", Theme.TextStyle.CAPTION, Theme.NAVY);
                open.setTouchable(Touchable.enabled);
                open.addListener(clicked(() -> openFolder(key)));
                line.add(open).top().padLeft(Theme.SPACE_3);
            }
            card.add(line).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    // ---- Folders

    private Table folders() {
        Map<String, String> names = rivalNames();
        if (folderKey == null || !names.containsKey(folderKey)) {
            folderKey = names.isEmpty() ? null : names.keySet().iterator().next();
        }
        Table columns = new Table();
        columns.top();
        Table list = ui.card();
        list.top();
        list.defaults().growX().left();
        list.add(sectionTitle("Rival agencies")).padBottom(Theme.SPACE_3).row();
        if (names.isEmpty()) {
            list.add(ui.paragraph(session.rivals() == null && !rivalsFailed ? "Fetching the agencies…"
                : "No rival agencies yet.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (Map.Entry<String, String> name : names.entrySet()) {
            list.add(folderTab(name.getKey(), name.getValue())).padBottom(Theme.SPACE_2).row();
        }
        columns.add(list).width(360f).top().padRight(COLUMN_GAP);
        columns.add(folderKey == null ? new Table() : folder(folderKey, names.get(folderKey))).expandX().fillX().top();
        return columns;
    }

    private Table folderTab(String key, String name) {
        RivalFolder folder = intel().getFolders().get(key);
        boolean on = key.equals(folderKey);
        Table tab = new Table();
        tab.setBackground(ui.optionCard(on));
        tab.pad(Theme.SPACE_3);
        tab.left();
        tab.defaults().left();
        tab.add(ui.label(name, Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        Optional<RivalSummary> summary = rival(key);
        summary.ifPresent(rival -> tab.add(ui.label("Dir. " + rival.directorName() + " · "
            + Museum.archetype(rival.archetype()) + " · " + goals(rival), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).row());
        tab.add(ui.label(folder == null || folder.getKnownLevel() < 0 ? "Intelligence level unknown"
            : "Level " + folder.getKnownLevel() + " as of " + Museum.date(folder.getKnownLevelTurn()),
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        tab.add(ui.paragraph(dossierDates(folder), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        tab.setTouchable(Touchable.enabled);
        tab.addListener(clicked(() -> openFolder(key)));
        return tab;
    }

    private static String dossierDates(RivalFolder folder) {
        if (folder == null) {
            return "Nothing on file";
        }
        List<String> parts = new ArrayList<>();
        for (IntelArea each : IntelArea.values()) {
            Dossier dossier = folder.getDossiers().get(each);
            if (dossier != null) {
                parts.add(IntelTexts.areaTitle(each) + " " + Museum.date(dossier.turn()));
            }
        }
        if (parts.isEmpty()) {
            return folder.getTidbits().isEmpty() ? "Nothing on file" : "Tidbits only · newest "
                + Museum.date(folder.getTidbits().getLast().turn());
        }
        return String.join(" · ", parts);
    }

    private Table folder(String key, String name) {
        RivalFolder folder = intel().getFolders().get(key);
        Table column = column();
        Table header = ui.card();
        header.left();
        header.defaults().left();
        header.add(ui.label("FOLDER", Theme.TextStyle.EYEBROW, Theme.GOLD_TEXT)).row();
        header.add(ui.label(name, Theme.TextStyle.HEADING_LG, Theme.INK)).padBottom(Theme.SPACE_1).row();
        rival(key).ifPresent(rival -> header.add(ui.label("Dir. " + rival.directorName() + " · "
            + Museum.archetype(rival.archetype()) + " archetype · " + goals(rival) + " reached",
            Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row());
        header.add(ui.label(IntelTexts.knownLevel(folder), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        if (folder != null && folder.getKnownLevel() >= 0) {
            header.add(ui.label(capitalise(IntelTexts.levelSource(folder)) + " · your operations against them: succeed "
                + IntelTexts.odds(folder) + ", caught " + IntelTexts.caughtOdds(folder), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).row();
        }
        column.add(header).padBottom(COLUMN_GAP).row();

        Table grid = new Table();
        grid.top();
        Table first = column();
        first.add(dossierCard(folder, IntelArea.RND)).padBottom(COLUMN_GAP).row();
        first.add(dossierCard(folder, IntelArea.PLANS)).row();
        Table second = column();
        second.add(dossierCard(folder, IntelArea.PERSONNEL)).padBottom(COLUMN_GAP).row();
        second.add(filesBlock(key, folder)).row();
        Table third = column();
        third.add(tidbitsCard(folder)).padBottom(COLUMN_GAP).row();
        third.add(caughtCard(folder)).row();
        grid.add(first).expandX().fillX().uniformX().top().padRight(COLUMN_GAP);
        grid.add(second).expandX().fillX().uniformX().top().padRight(COLUMN_GAP);
        grid.add(third).expandX().fillX().uniformX().top();
        column.add(grid).row();
        return column;
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private Table stamp(String text, boolean none) {
        Table stamp = new Table();
        stamp.setBackground(ui.rect(Theme.SURFACE_CARD, none ? Theme.NAVY_BORDER : Theme.NAVY, Theme.BORDER_EMPHASIS));
        stamp.pad(2f, 6f, 2f, 6f);
        stamp.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, none ? Theme.INK_MUTED : Theme.NAVY));
        return stamp;
    }

    private Table dossierCard(RivalFolder folder, IntelArea which) {
        Dossier dossier = folder == null ? null : folder.getDossiers().get(which);
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        head.add(sectionTitle(IntelTexts.areaTitle(which))).expandX().left();
        head.add(stamp(dossier == null ? "Never assessed" : IntelTexts.asOf(dossier.turn()), dossier == null));
        card.add(head).padBottom(Theme.SPACE_3).row();
        if (dossier == null) {
            card.add(ui.paragraph("No assessment yet: " + Character.toLowerCase(IntelTexts.areaCovers(which).charAt(0))
                + IntelTexts.areaCovers(which).substring(1), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
            if (intel().getLevel() >= Intelligence.ASSESSMENT_LEVEL) {
                Label queue = ui.label("Queue one in Operations", Theme.TextStyle.CAPTION, Theme.NAVY);
                queue.setTouchable(Touchable.enabled);
                queue.addListener(clicked(() -> {
                    rivalKey = folderKey;
                    kind = IntelOperation.Kind.ASSESSMENT;
                    area = which;
                    select(Tab.OPERATIONS);
                }));
                card.add(queue).padTop(Theme.SPACE_2).row();
            }
            return card;
        }
        if (which == IntelArea.RND) {
            for (IntelFact fact : dossier.facts()) {
                card.add(rangeRow(fact)).padBottom(Theme.SPACE_2).row();
            }
            card.add(ui.paragraph("Reliability somewhere in the band; the gold mark is each component's R&D cap.",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        } else if (which == IntelArea.PERSONNEL) {
            card.add(personnelTable(dossier)).row();
        } else {
            Map<String, List<String>> rows = new LinkedHashMap<>();
            for (IntelFact fact : dossier.facts()) {
                String[] row = IntelTexts.row(fact);
                rows.computeIfAbsent(row[0], label -> new ArrayList<>()).add(row[1]);
            }
            Table kv = new Table();
            kv.defaults().left().top().padBottom(Theme.SPACE_1);
            for (Map.Entry<String, List<String>> row : rows.entrySet()) {
                kv.add(sectionTitle(row.getKey())).width(130f).padRight(Theme.SPACE_3);
                kv.add(ui.paragraph(String.join(", ", row.getValue()), Theme.TextStyle.BODY_SM, Theme.INK)).expandX()
                    .fillX().row();
            }
            if (rows.isEmpty()) {
                kv.add(ui.paragraph("Nothing to report.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED));
            }
            card.add(kv).row();
            if (which == IntelArea.PLANS) {
                card.add(ui.paragraph("What they fly next may have changed since.", Theme.TextStyle.CAPTION_ITALIC,
                    Theme.INK_MUTED)).padTop(Theme.SPACE_1).row();
            }
        }
        return card;
    }

    /**
     * A personnel dossier as a small table: a row per group, its head count and its average skill
     * (a range), so long group names never run into the numbers.
     */
    private Table personnelTable(Dossier dossier) {
        Table table = new Table();
        table.defaults().left().padBottom(Theme.SPACE_1);
        table.add().expandX().fillX();
        Label people = sectionTitle("People");
        people.setAlignment(Align.right);
        table.add(people).right().padRight(Theme.SPACE_4);
        Label skill = sectionTitle("Average skill");
        skill.setAlignment(Align.right);
        table.add(skill).right().row();
        for (PersonnelGroup each : PersonnelGroup.values()) {
            IntelFact count = null;
            IntelFact average = null;
            for (IntelFact fact : dossier.facts()) {
                if (each.name().equals(fact.ref())) {
                    if (fact.kind() == IntelFact.Kind.HEADCOUNT) {
                        count = fact;
                    } else if (fact.kind() == IntelFact.Kind.AVERAGE_SKILL) {
                        average = fact;
                    }
                }
            }
            if (count == null && average == null) {
                continue;
            }
            table.add(ui.label(IntelTexts.groupName(each.name()), Theme.TextStyle.BODY_SM, Theme.INK)).expandX()
                .fillX();
            table.add(ui.label(count == null ? "—" : String.valueOf(count.count()), Theme.TextStyle.BODY_STRONG,
                Theme.INK)).right().padRight(Theme.SPACE_4);
            table.add(ui.label(average == null ? "—" : average.low() + "–" + average.high(), Theme.TextStyle.BODY_STRONG,
                Theme.INK)).right().row();
        }
        return table;
    }

    private Table rangeRow(IntelFact fact) {
        Table row = new Table();
        row.add(ui.label(IntelTexts.row(fact)[0], Theme.TextStyle.BODY_SM, Theme.INK)).width(150f).left()
            .padRight(Theme.SPACE_2);
        Optional<MissionComponent> component = session.catalog().missionComponent(fact.ref());
        double max = component.map(MissionComponent::maxReliability).orElse(100.0);
        double cap = component.map(MissionComponent::maxRdReliability).orElse(max);
        row.add(new ReliabilityBar(ui, 0, cap, max, 0).range(ui, fact.low(), fact.high())).expandX().fillX()
            .padRight(Theme.SPACE_2);
        Label value = ui.label(fact.low() + "–" + fact.high() + " %", Theme.TextStyle.BODY_STRONG, Theme.INK);
        value.setAlignment(Align.right);
        row.add(value).width(70f);
        return row;
    }

    private Table filesBlock(String key, RivalFolder folder) {
        Table block = column();
        Table head = new Table();
        head.add(ui.label("PERSONNEL FILES", Theme.TextStyle.EYEBROW, Theme.GOLD_ON_PAGE)).expandX().left();
        head.add(ui.label("Level " + Intelligence.PERSONNEL_FILE_LEVEL + " operations", Theme.TextStyle.CAPTION,
            Theme.ON_PAGE_MUTED));
        block.add(head).padBottom(Theme.SPACE_2).row();
        List<PersonnelFile> files = folder == null ? List.of() : folder.getPersonnelFiles();
        if (files.isEmpty()) {
            Table card = ui.card();
            card.add(ui.paragraph("No files yet. A level " + Intelligence.PERSONNEL_FILE_LEVEL + " service can open one "
                + "on a rival's engineer, astronaut or controller.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).growX();
            block.add(card).row();
        }
        for (int i = files.size() - 1; i >= 0; i--) {
            block.add(fileCard(key, files.get(i))).padBottom(Theme.SPACE_3).row();
        }
        return block;
    }

    private Table fileCard(String key, PersonnelFile file) {
        IntelOperation queued = queuedOffer(key, file.personId());
        Table card = ui.card();
        if (queued != null) {
            card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.GOLD, Theme.BORDER_EMPHASIS));
        }
        card.top();
        card.defaults().growX().left();
        Table head = new Table();
        Drawable portrait = file.portraitId() == null ? null : ui.picture("portraits/" + file.portraitId() + ".jpg");
        head.add(new Image(portrait == null ? ui.solid(Theme.NAVY) : portrait, Scaling.fill)).size(48f)
            .padRight(Theme.SPACE_3);
        Table who = new Table();
        who.defaults().left();
        who.add(ui.label(file.name(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        who.add(ui.label(IntelTexts.groupOne(file.group()) + " · age " + file.age(), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).row();
        if (file.tenureSeasons() > 0) {
            who.add(ui.label(PoachTexts.since(file, agencyName(key)), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        head.add(who).expandX().left();
        head.add(stamp(IntelTexts.asOf(file.turn()), false)).top();
        card.add(head).padBottom(Theme.SPACE_3).row();
        List<IntelFact> best = file.skills().stream()
            .sorted((a, b) -> Integer.compare(b.low() + b.high(), a.low() + a.high())).limit(3).toList();
        for (IntelFact skill : best) {
            Table row = new Table();
            row.add(ui.label(IntelTexts.row(skill)[0], Theme.TextStyle.CAPTION, Theme.INK)).width(130f).left()
                .padRight(Theme.SPACE_2);
            row.add(new ReliabilityBar(ui, 0, 0, 100, 0).range(ui, skill.low(), skill.high())).expandX().fillX()
                .padRight(Theme.SPACE_2);
            row.add(ui.label(skill.low() + "–" + skill.high(), Theme.TextStyle.CAPTION, Theme.INK)).width(44f);
            card.add(row).padBottom(2f).row();
        }
        Table trait = new Table();
        trait.add(sectionTitle("A trait")).width(80f).left().padRight(Theme.SPACE_2);
        if (file.trait() == null) {
            trait.add(ui.label("No notable traits", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).left();
        } else {
            trait.add(new TraitChip(ui, file.trait())).height(TraitChip.HEIGHT).left();
        }
        trait.add().expandX();
        card.add(trait).padTop(Theme.SPACE_2).row();
        Table more = new Table();
        more.defaults().left().top().padBottom(2f);
        more.add(sectionTitle("Loyalty")).width(80f).padRight(Theme.SPACE_2);
        more.add(ui.label(file.loyalty().isEmpty() ? "Not known" : file.loyalty(), Theme.TextStyle.BODY_SM, Theme.INK))
            .expandX().row();
        more.add(sectionTitle("Ambition")).width(80f).padRight(Theme.SPACE_2);
        String ambition = file.ambition() != null
            ? de.mkoehler.launchwindow.client.view.CareerTexts.ambition(file.ambition().kind(), file.ambition().ref())
                + " · " + de.mkoehler.launchwindow.client.view.CareerTexts.seasonsLeft(file.ambition().seasonsLeft())
            : file.disappointed() ? "Disappointed: an ambition ran out lately" : "None said";
        more.add(ui.paragraph(ambition, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX().row();
        card.add(more).padTop(Theme.SPACE_2).row();
        card.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(1f).padTop(Theme.SPACE_3).padBottom(Theme.SPACE_3).row();
        card.add(offerFooter(key, file, queued)).row();
        return card;
    }

    private IntelOperation queuedOffer(String key, String personId) {
        return Poaching.queuedOffers(session.state()).stream()
            .filter(offer -> offer.targetId().equals(key) && offer.personId().equals(personId)).findFirst().orElse(null);
    }

    /**
     * A file card's footer: the offer button, why there can't be one, or the queued offer
     * ({@code ui-spec.md} &sect;4.16, Poaching canvas).
     */
    private Table offerFooter(String key, PersonnelFile file, IntelOperation queued) {
        Table footer = new Table();
        footer.left();
        footer.defaults().left();
        if (queued != null) {
            footer.add(queuedBadge(queued.raisePercent())).row();
            footer.add(ui.paragraph(PoachTexts.ifAccepted(file, queued.raisePercent()), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).growX().padTop(Theme.SPACE_1).row();
            Label cancel = ui.label("Cancel the offer", Theme.TextStyle.CAPTION, Theme.DANGER);
            cancel.setTouchable(Touchable.enabled);
            cancel.addListener(clicked(() -> cancelOffer(queued)));
            footer.add(cancel).padTop(Theme.SPACE_1);
            return footer;
        }
        Poaching.Blocker blocker = Poaching.blocker(file, (int) session.latestResolvedTurn() + 1);
        if (blocker != null) {
            footer.add(reason(blocker == Poaching.Blocker.ASTRONAUT ? "lock" : "clock",
                PoachTexts.blocker(blocker, file, agencyName(key)))).growX();
            return footer;
        }
        if (intel().getLevel() < Intelligence.PERSONNEL_FILE_LEVEL) {
            footer.add(reason("lock", "Offers need the service at level " + Intelligence.PERSONNEL_FILE_LEVEL + "."))
                .growX();
            return footer;
        }
        List<IntelOperation> offers = Poaching.queuedOffers(session.state());
        boolean used = offers.size() >= Poaching.OFFERS_PER_SEASON;
        footer.add(offerButton(!used, () -> openOffer(key, file))).row();
        if (used) {
            IntelOperation other = offers.getFirst();
            PersonnelFile to = Poaching.file(session.state(), other.targetId(), other.personId());
            footer.add(reason("clock", "Your offer this season goes to " + (to == null ? "someone else" : to.name())
                + ".")).growX().padTop(Theme.SPACE_2);
        }
        return footer;
    }

    private Table reason(String icon, String text) {
        Table line = new Table();
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(Theme.INK_MUTED);
        line.add(glyph).size(12f).top().padTop(2f).padRight(Theme.SPACE_2);
        line.add(ui.paragraph(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().fillX();
        return line;
    }

    private Table offerButton(boolean enabled, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(enabled ? Theme.NAVY : Theme.SURFACE_CARD, enabled ? Theme.NAVY
            : Theme.NAVY_BORDER, Theme.BORDER_THIN));
        button.pad(Theme.SPACE_2, Theme.SPACE_3, Theme.SPACE_2, Theme.SPACE_3);
        Color color = enabled ? Theme.ON_FILL : Theme.INK_MUTED;
        Image funds = new Image(ui.icon("funds"), Scaling.fit);
        funds.setColor(color);
        button.add(funds).size(12f).padRight(Theme.SPACE_2);
        button.add(ui.label("Make an offer", Theme.TextStyle.LABEL, color));
        if (enabled) {
            button.setTouchable(Touchable.enabled);
            button.addListener(clicked(action));
        }
        return button;
    }

    // ---- The offer dialog

    /**
     * Opens the offer dialog for a person in a file.
     *
     * @param key  the rival's key
     * @param file the file
     */
    public void openOffer(String key, PersonnelFile file) {
        String agency = agencyName(key);
        Table card = dialogCard("Intelligence Office · an offer to " + agency + "'s staff",
            "Make " + file.name() + " an offer");
        Table columns = new Table();
        columns.top();
        columns.add(offerFileColumn(key, file)).width(470f).top().padRight(Theme.SPACE_4);
        columns.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(1f).growY().padRight(Theme.SPACE_4);
        columns.add(offerTermsColumn(key, file, agency)).expandX().fillX().top();
        card.add(columns).growX().row();
        showDialog(card, 1260f);
    }

    private Table offerFileColumn(String key, PersonnelFile file) {
        Table column = column();
        Table head = new Table();
        Drawable portrait = file.portraitId() == null ? null : ui.picture("portraits/" + file.portraitId() + ".jpg");
        head.add(new Image(portrait == null ? ui.solid(Theme.NAVY) : portrait, Scaling.fill)).size(96f).top()
            .padRight(Theme.SPACE_3);
        Table who = new Table();
        who.defaults().left();
        who.add(ui.label(file.name(), Theme.TextStyle.HEADING_SM, Theme.INK)).row();
        who.add(ui.label(IntelTexts.groupOne(file.group()) + " · age " + file.age()
            + (file.salary() > 0 ? " · salary " + Math.round(file.salary()) : ""), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).padBottom(Theme.SPACE_1).row();
        who.add(stamp(IntelTexts.asOf(file.turn()), false)).padBottom(Theme.SPACE_1).row();
        who.add(ui.label("What your file says", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        head.add(who).expandX().left().top();
        column.add(head).padBottom(Theme.SPACE_3).row();
        List<IntelFact> best = file.skills().stream()
            .sorted((a, b) -> Integer.compare(b.low() + b.high(), a.low() + a.high())).limit(3).toList();
        for (IntelFact skill : best) {
            Table row = new Table();
            row.add(ui.label(IntelTexts.row(skill)[0], Theme.TextStyle.BODY_SM, Theme.INK)).width(170f).left()
                .padRight(Theme.SPACE_2);
            row.add(new ReliabilityBar(ui, 0, 0, 100, 0).range(ui, skill.low(), skill.high())).expandX().fillX()
                .padRight(Theme.SPACE_2);
            row.add(ui.label(skill.low() + "–" + skill.high(), Theme.TextStyle.BODY_SM, Theme.INK)).width(50f);
            column.add(row).padBottom(Theme.SPACE_1).row();
        }
        Table more = new Table();
        more.defaults().left().top().padBottom(Theme.SPACE_1);
        more.add(sectionTitle("Loyalty")).width(100f).padRight(Theme.SPACE_2);
        more.add(ui.label(file.loyalty().isEmpty() ? "Not known" : file.loyalty(), Theme.TextStyle.BODY_SM, Theme.INK))
            .expandX().row();
        more.add(sectionTitle("Ambition")).width(100f).padRight(Theme.SPACE_2);
        Table ambition = new Table();
        ambition.left();
        String said = file.ambition() == null ? "None said"
            : de.mkoehler.launchwindow.client.view.CareerTexts.ambition(file.ambition().kind(), file.ambition().ref())
            + (file.disappointed() ? "" : " · " + de.mkoehler.launchwindow.client.view.CareerTexts.seasonsLeft(
                file.ambition().seasonsLeft()));
        ambition.add(ui.label(said, Theme.TextStyle.BODY_SM, Theme.INK));
        if (file.disappointed()) {
            ambition.add(ui.label(" · disappointed", Theme.TextStyle.BODY_SM, Theme.DANGER));
        }
        more.add(ambition).expandX().left().row();
        more.add(sectionTitle("A trait")).width(100f).padRight(Theme.SPACE_2);
        if (file.trait() == null) {
            more.add(ui.label("No notable traits", Theme.TextStyle.CAPTION_ITALIC, Theme.INK_MUTED)).left();
        } else {
            more.add(new TraitChip(ui, file.trait())).height(TraitChip.HEIGHT).left();
        }
        column.add(more).padTop(Theme.SPACE_2).row();
        return column;
    }

    private Table offerTermsColumn(String key, PersonnelFile file, String agency) {
        Table column = column();
        Table head = new Table();
        head.add(sectionTitle("The raise over today's salary")).expandX().left();
        head.add(ui.label("Offer: " + Poaching.queuedOffers(session.state()).size() + " of " + Poaching.OFFERS_PER_SEASON
            + " used this season", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        column.add(head).padBottom(Theme.SPACE_2).row();
        Table segments = new Table();
        for (int raise : Poaching.RAISE_PERCENTS) {
            boolean on = raise == offerRaise;
            Table segment = new Table();
            segment.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
            segment.pad(Theme.SPACE_2);
            segment.add(ui.label(PoachTexts.raise(raise), Theme.TextStyle.BODY_STRONG, on ? Theme.ON_FILL : Theme.NAVY))
                .row();
            segment.add(ui.label(Poaching.offeredSalary(file.salary(), raise) + " a season", Theme.TextStyle.CAPTION,
                on ? Theme.ON_FILL : Theme.INK_MUTED));
            segment.setTouchable(Touchable.enabled);
            segment.addListener(clicked(() -> {
                offerRaise = raise;
                closeDialog();
                openOffer(key, file);
            }));
            segments.add(segment).expandX().fillX().uniformX();
        }
        column.add(segments).padBottom(Theme.SPACE_4).row();

        long salary = Poaching.offeredSalary(file.salary(), offerRaise);
        column.add(sectionTitle("What it costs if they come")).padBottom(Theme.SPACE_2).row();
        Table costs = new Table();
        costs.defaults().left().top().padBottom(Theme.SPACE_2);
        costs.add(sectionTitle("New salary")).width(160f);
        costs.add(ui.label("$ " + PoachTexts.money(salary) + " a season, from next season", Theme.TextStyle.BODY_STRONG,
            Theme.INK)).expandX().row();
        costs.add(sectionTitle("Signing bonus")).width(160f);
        costs.add(ui.label("$ " + PoachTexts.money(Poaching.signingBonus(salary)) + " once, when they join",
            Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        PersonnelGroup group = PersonnelGroup.valueOf(file.group());
        int size = group == PersonnelGroup.SET ? session.state().getSetSpecialists().size()
            : session.state().getMissionControlOfficers().size();
        int cap = capacity(group);
        boolean full = size >= cap;
        costs.add(sectionTitle("Room")).width(160f);
        Table room = new Table();
        room.left();
        room.defaults().left();
        room.add(ui.label(room(file.group()), Theme.TextStyle.BODY_STRONG, full ? Theme.DANGER : Theme.INK)).row();
        room.add(ui.paragraph(full ? "The offer lapses unless a place is free at the resolution. Dismiss someone or "
            + "finish an upgrade before then." : "Room for " + (cap - size) + " more.", Theme.TextStyle.CAPTION,
            full ? Theme.DANGER : Theme.INK_MUTED)).growX();
        costs.add(room).expandX().fillX().row();
        column.add(costs).row();
        column.add(ui.label("Nothing is paid unless they come.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_4).row();

        int[] range = Poaching.answerRange(offerRaise, file);
        column.add(sectionTitle("Their answer, as far as you can tell")).padBottom(Theme.SPACE_1).row();
        Table answer = new Table();
        answer.left();
        answer.add(ui.label(PoachTexts.range(range), Theme.TextStyle.HEADING_LG, Theme.INK)).padRight(Theme.SPACE_2);
        answer.add(ui.label("chance they accept, if their agency doesn't match", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).bottom().padBottom(6f);
        column.add(answer).left().row();
        column.add(new ReliabilityBar(ui, 0, Poaching.ACCEPT_MAX_PERCENT, 100, 0).range(ui, range[0], range[1]))
            .height(10f).padTop(Theme.SPACE_1).row();
        Table scale = new Table();
        scale.add(ui.label("0 %", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        scale.add(ui.label(Poaching.ACCEPT_MAX_PERCENT + " %, the most anyone ever is", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED));
        column.add(scale).padBottom(Theme.SPACE_2).row();
        column.add(ui.paragraph("From their loyalty band and what the file shows; their morale and any trait the file "
            + "doesn't show aren't counted. " + agency + " may match the offer to keep them, and you can't see how far "
            + agency + " matches.", Theme.TextStyle.BODY_SM, Theme.INK)).growX().padBottom(Theme.SPACE_3).row();
        column.add(new Image(ui.solid(Theme.NAVY_BORDER))).height(1f).padBottom(Theme.SPACE_3).row();
        Table buttons = new Table();
        buttons.add(reason("clock", "Answer in the next turn report.")).expandX().fillX().left();
        buttons.add(actionButton("Cancel", Theme.ButtonKind.SECONDARY, this::closeDialog)).padRight(Theme.SPACE_2);
        buttons.add(actionButton("Make the offer · " + PoachTexts.raise(offerRaise), Theme.ButtonKind.PRIMARY,
            () -> makeOffer(key, file)));
        column.add(buttons).row();
        return column;
    }

    /**
     * Picks the raise the offer dialog shows next, as a click on its segment does.
     *
     * @param raisePercent the raise
     */
    public void pickRaise(int raisePercent) {
        offerRaise = raisePercent;
    }

    /**
     * Queues the offer the dialog shows.
     *
     * @param key  the rival's key
     * @param file the file
     */
    public void makeOffer(String key, PersonnelFile file) {
        submit(new MakeOfferAction(key, file.personId(), offerRaise), result -> {
            closeDialog();
            toast("Offer queued. Their answer comes in the next turn report.", Theme.NoticeKind.SUCCESS);
        });
    }

    private Table tidbitsCard(RivalFolder folder) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(sectionTitle("Tidbits")).padBottom(Theme.SPACE_3).row();
        List<Tidbit> tidbits = folder == null ? List.of() : folder.getTidbits();
        if (tidbits.isEmpty()) {
            card.add(ui.paragraph("None yet. From level " + Intelligence.ASSESSMENT_LEVEL + " the service picks some up "
                + "on its own.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        for (int i = tidbits.size() - 1; i >= 0; i--) {
            Tidbit tidbit = tidbits.get(i);
            Table line = new Table();
            line.add(ui.label(Museum.date(tidbit.turn()), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(90f).top()
                .left().padRight(Theme.SPACE_2);
            line.add(ui.paragraph(IntelTexts.tidbit(tidbit.fact()), Theme.TextStyle.BODY_SM, Theme.INK)).expandX()
                .fillX();
            card.add(line).padBottom(Theme.SPACE_2).row();
        }
        return card;
    }

    private Table caughtCard(RivalFolder folder) {
        Table card = ui.card();
        card.top();
        card.defaults().growX().left();
        card.add(sectionTitle("Caught")).padBottom(Theme.SPACE_3).row();
        List<IntelCatch> catches = folder == null ? List.of() : folder.getCatches();
        boolean anyOurs = false;
        boolean anyTheirs = false;
        for (IntelCatch caught : catches) {
            anyOurs |= caught.ours();
            anyTheirs |= !caught.ours();
            Table line = new Table();
            line.add(ui.label(Museum.date(caught.turn()), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(90f).top()
                .left().padRight(Theme.SPACE_2);
            String what = IntelTexts.operation(caught.kind(), caught.area(), null).toLowerCase(Locale.ROOT);
            String article = what.startsWith("a") ? "An " : "A ";
            line.add(ui.paragraph(caught.ours()
                ? "Yours, by them. " + article + what + " was blown: −" + Intelligence.CAUGHT_PRESTIGE + " prestige. Their "
                    + "counter-intelligence was level " + caught.level() + " that season."
                : "Theirs, by you. Their agent on " + article.toLowerCase(Locale.ROOT) + what + " was caught. Their Intelligence level was "
                    + caught.level() + ".", Theme.TextStyle.BODY_SM, Theme.INK)).expandX().fillX();
            card.add(line).padBottom(Theme.SPACE_2).row();
        }
        if (!anyOurs) {
            card.add(ui.label("None of yours caught so far.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(
                Theme.SPACE_1).row();
        }
        if (!anyTheirs) {
            card.add(ui.label("None of theirs caught so far.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        }
        return card;
    }

    // ---- Rivals

    private List<RivalSummary> rivals() {
        List<RivalSummary> all = session.rivals();
        if (all == null) {
            return List.of();
        }
        return all.stream().filter(rival -> !rival.you() && !rival.playerId().isEmpty()).toList();
    }

    private Optional<RivalSummary> rival(String key) {
        return rivals().stream().filter(rival -> rival.playerId().equals(key)).findFirst();
    }

    /**
     * Returns every rival the office knows of, by key: the race's agencies first, then any folder
     * whose agency has left the race.
     */
    private Map<String, String> rivalNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (RivalSummary rival : rivals()) {
            names.put(rival.playerId(), rival.agencyName());
        }
        intel().getFolders().forEach((key, folder) -> names.putIfAbsent(key, folder.getAgencyName()));
        return names;
    }

    private static String goals(RivalSummary rival) {
        int count = rival.goalTurns().size();
        return count + (count == 1 ? " goal" : " goals");
    }

    private String agencyName(String key) {
        String name = rivalNames().get(key);
        return name == null ? "Unknown agency" : name;
    }

    private static int newest(RivalFolder folder) {
        if (folder == null) {
            return -1;
        }
        int newest = -1;
        for (Dossier dossier : folder.getDossiers().values()) {
            newest = Math.max(newest, dossier.turn());
        }
        for (PersonnelFile file : folder.getPersonnelFiles()) {
            newest = Math.max(newest, file.turn());
        }
        for (Tidbit tidbit : folder.getTidbits()) {
            newest = Math.max(newest, tidbit.turn());
        }
        return newest;
    }
}
