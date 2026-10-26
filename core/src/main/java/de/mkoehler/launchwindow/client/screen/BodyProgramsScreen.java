package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.GoalModifiers;
import de.mkoehler.launchwindow.client.view.BodyMap;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.MoonRoads;
import de.mkoehler.launchwindow.client.view.Outlook;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;

import java.util.List;

/**
 * A body's program categories and programs ({@code ui-spec.md} &sect;4.6.3): the category list
 * with its counts on the left, the selected category's programs as cards on the right, each with
 * its state (open, available, locked with the missing goals), its price and upkeep, and Details.
 *
 * @author Mario Koehler
 */
public final class BodyProgramsScreen extends ChromeScreen {

    private static final float CARD_ART_RATIO = 432f / 672f;

    private final CelestialBody body;
    private String selectedCategoryId;

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     * @param body the body whose programs to show
     */
    public BodyProgramsScreen(LaunchWindowGame game, CelestialBody body) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.HEADQUARTERS,
            new Crumb(BodyMap.name(body), null)), "Headquarters · " + BodyMap.name(body),
            "Program categories and programs");
        this.body = body;
        List<ProgramCategory> categories = Programs.categories(body, session.state(), session.catalog());
        selectedCategoryId = categories.isEmpty() ? null : categories.get(0).id();
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(new SelectBodyScreen(game));
    }

    /**
     * Selects a category, as clicking it does.
     *
     * @param categoryId the category's id
     */
    public void select(String categoryId) {
        selectedCategoryId = categoryId;
        rebuild();
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.HEADQUARTERS;
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        List<ProgramCategory> categories = Programs.categories(body, session.state(), session.catalog());

        Table left = new Table();
        left.top();
        left.defaults().growX();
        left.add(ui.label("Categories", Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).left().padBottom(Theme.SPACE_2)
            .row();
        for (ProgramCategory category : categories) {
            left.add(categoryButton(category)).padBottom(Theme.SPACE_2).row();
        }
        content.add(left).width(400f).top().fillY().padRight(Theme.SPACE_5);

        Table right = new Table();
        right.top();
        ProgramCategory selected = categories.stream().filter(c -> c.id().equals(selectedCategoryId)).findFirst()
            .orElse(null);
        if (selected == null) {
            right.add(ui.label("No programs here for your agency.", Theme.TextStyle.BODY, Theme.ON_PAGE_STRONG)).left();
            content.add(right).expand().fill();
            return;
        }
        List<Program> programs = Programs.inCategory(selected, session.state(), session.catalog());
        Table header = new Table();
        header.add(ui.label(ContentNames.of(selected.id(), selected.name()) + " · " + programs.size() + (programs.size() == 1 ? " program"
            : " programs"), Theme.TextStyle.EYEBROW, Theme.ON_PAGE_STRONG)).left().expandX();
        // The roads to the Moon compared (ui-spec.md 4.21), from the category that holds them, locked or not.
        MoonRoads roads = MoonRoads.standard();
        if (programs.stream().anyMatch(program -> roads.roads().stream().anyMatch(road -> road.program()
                .equals(program.id())))) {
            header.add(onClick(ui.pageButton("Compare the roads to the Moon"), () -> game.setScreen(new RoadsToMoonScreen(game))))
                .right();
        }
        right.add(header).growX().padBottom(Theme.SPACE_2).row();
        Table grid = new Table();
        grid.top().left();
        float cardWidth = (1920f - 2 * Theme.SPACE_6 - 400f - Theme.SPACE_5 - 2 * Theme.SPACE_4) / 3f;
        for (int i = 0; i < programs.size(); i++) {
            grid.add(programCard(programs.get(i), cardWidth)).width(cardWidth).top()
                .padRight(i % 3 < 2 ? Theme.SPACE_4 : 0f).padBottom(Theme.SPACE_4);
            if (i % 3 == 2) {
                grid.row();
            }
        }
        ScrollPane scroll = scrollPane(grid);
        right.add(scroll).expand().fill();
        content.add(right).expand().fill();
    }

    private Table categoryButton(ProgramCategory category) {
        boolean active = category.id().equals(selectedCategoryId);
        Table button = new Table();
        button.setBackground(active
            ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.GOLD, 0.1f), Theme.GOLD, Theme.BORDER_EMPHASIS)
            : ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        button.pad(Theme.SPACE_2);
        Drawable art = ui.picture("categories/" + category.id() + "_" + session.state().getArchetype().name() + ".jpg");
        button.add(art == null ? new Image(ui.solid(Theme.NAVY)) : new Image(art, Scaling.fill)).size(112f, 72f)
            .padRight(Theme.SPACE_3);
        Table text = new Table();
        text.left();
        text.defaults().left();
        text.add(ui.label(ContentNames.of(category.id(), category.name()), Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(4f).row();
        text.add(ui.label(Programs.counts(Programs.inCategory(category, session.state(), session.catalog()),
            session.state()).text(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        button.add(text).expandX().left();
        button.setTouchable(Touchable.enabled);
        button.addListener(clicked(() -> select(category.id())));
        return button;
    }

    /**
     * Adds a card's goal lines, what goals not reached cost and what reached ones give, each with the
     * goals in a tooltip. Nothing when neither applies.
     *
     * @param host    the screen
     * @param body    the card's body
     * @param goals   the effects
     * @param program whether it's a program's card
     */
    static void goalLines(ChromeScreen host, Table body, GoalModifiers.Summary goals, boolean program) {
        if (goals.isEmpty()) {
            return;
        }
        Table lines = new Table();
        lines.left();
        String missing = goals.missingText(program);
        if (missing != null) {
            lines.add(host.ui.label(missing, Theme.TextStyle.CAPTION, Theme.DANGER)).left().padRight(Theme.SPACE_3);
        }
        String reached = goals.reachedText(program);
        if (reached != null) {
            lines.add(host.ui.label(reached, Theme.TextStyle.CAPTION, Theme.SUCCESS)).left();
        }
        host.ui.tooltip(lines, program ? "Goals and reliability" : "Goals and this mission's reliability",
            goals.detail(program));
        body.add(lines).left().padBottom(Theme.SPACE_2).row();
    }

    private void goalLines(Table body, GoalModifiers.Summary goals, boolean program) {
        goalLines(this, body, goals, program);
    }

    private Table programCard(Program program, float width) {
        Programs.State state = Programs.state(program, session.state());
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        card.pad(Theme.BORDER_THIN);
        card.top();
        Drawable art = ui.picture("programs/" + program.id() + ".jpg");
        Image image = art == null ? new Image(ui.solid(Theme.NAVY)) : new Image(art, Scaling.fill);
        if (state == Programs.State.LOCKED) {
            image.setColor(0.55f, 0.55f, 0.55f, 0.55f);
        }
        float inner = width - 2 * Theme.BORDER_THIN;
        card.add(image).size(inner, inner * CARD_ART_RATIO).row();

        Table body = new Table();
        body.pad(Theme.SPACE_3, Theme.SPACE_4, Theme.SPACE_4, Theme.SPACE_4);
        body.defaults().growX().left();
        Table head = new Table();
        head.add(ui.label(ContentNames.of(program.id()), Theme.TextStyle.HEADING_SM, Theme.INK)).expandX().left();
        head.add(programStateMark(state)).right();
        body.add(head).padBottom(Theme.SPACE_2).row();
        String price = state == Programs.State.OPEN ? "Open" : "Opening "
            + money(Programs.price(program, session.state(), session.catalog()).total());
        body.add(ui.label(price + " · Upkeep " + money(program.maintenanceCost()) + " / season",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table contractLine = ContractMarker.programLine(this, program.id());
        if (contractLine != null) {
            body.add(contractLine).padBottom(Theme.SPACE_2).row();
        }
        Table staffing = staffingLine(program);
        if (staffing != null) {
            body.add(staffing).padBottom(Theme.SPACE_2).row();
        }
        // The rockets its missions need, against the open ones (the user's request, 2026-10-10).
        Outlook.ProgramRockets rockets = Outlook.programRockets(program, session.state(), session.catalog());
        if (rockets != null) {
            Label line = ui.label((rockets.easiest() == rockets.hardest() ? "Rocket " : "Rockets ") + rockets.range(),
                Theme.TextStyle.CAPTION, switch (rockets.supply()) {
                    case READY -> Theme.SUCCESS;
                    case COMING -> Theme.GOLD_TEXT;
                    case SHORT -> Theme.DANGER;
                });
            ui.tooltip(line, "Rockets its missions need", "The smallest rocket each mission needs, from its easiest "
                + "to its hardest. Green: your open rockets fly all of them; amber: some; red: none yet.\n"
                + String.join("\n", rockets.lines()));
            body.add(line).padBottom(Theme.SPACE_2).row();
        }
        if (state == Programs.State.LOCKED) {
            Label needs = ui.paragraph("Needs: " + String.join(", ", Programs.missingGoals(program, session.state())
                .stream().map(ContentNames::of).toList()), Theme.TextStyle.BODY_SM, Theme.INK);
            ui.tooltip(needs, "Where to earn them", String.join("\n", Programs.missingGoals(program, session.state())
                .stream().map(goal -> ContentNames.of(goal) + ": " + Outlook.sourceLine(goal, session.state(),
                    session.catalog())).toList()));
            body.add(needs).padBottom(Theme.SPACE_2).row();
        }
        // What skipping ahead costs, and what earlier goals give (play test, 2026-10-09).
        GoalModifiers.Summary goals = GoalModifiers.ofProgram(program, session.catalog(),
            session.state()::hasAchievedOrCancelledGoal);
        goalLines(body, goals, true);
        body.add(actionButton("Details", Theme.ButtonKind.SECONDARY, () -> game.setScreen(
            new ProgramDetailsScreen(game, program.id(), this.body)))).left().fill(false, false);
        card.add(body).width(inner);
        return card;
    }

    /**
     * A program card's staffing line (the user's request, 2026-10-10): the astronauts and
     * controllers its missions need, smallest to largest, each green when enough are free now, amber
     * when the roster has enough but some are busy or training, red when it's too small.
     *
     * @param program the program
     * @return the line, or {@code null} for a program that needs nobody
     */
    private Table staffingLine(Program program) {
        List<Outlook.Need> needs = Outlook.needs(program, session.state(), session.catalog());
        if (needs.isEmpty()) {
            return null;
        }
        Table line = new Table();
        line.left();
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < needs.size(); i++) {
            Outlook.Need need = needs.get(i);
            if (i > 0) {
                line.add(ui.label(" · ", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
                detail.append("\n");
            }
            Color color = switch (need.supply()) {
                case READY -> Theme.SUCCESS;
                case COMING -> Theme.GOLD_TEXT;
                case SHORT -> Theme.DANGER;
            };
            String name = need.group().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + need.group().substring(1);
            line.add(ui.label(name + " " + need.range(), Theme.TextStyle.CAPTION, color));
            detail.append(name).append(": its missions need ").append(need.min() == need.max() ? need.min()
                : need.min() + " to " + need.max()).append(need.group().equals("astronauts") ? " (crew and CAPCOM)" : "")
                .append(need.min() == 0 ? "; some fly without any, the colour is for the smallest that needs them"
                    : "")
                .append(". You have ").append(need.free()).append(" free of ").append(need.roster()).append(". ")
                .append(switch (need.supply()) {
                    case READY -> "Enough for the smallest mission now.";
                    case COMING -> "Enough on the roster, but not all free: some are training or busy.";
                    case SHORT -> "Too few even on the roster: hire first. Basic training comes after hiring.";
                });
        }
        ui.tooltip(line, "People its missions need", detail.toString());
        return line;
    }
}
