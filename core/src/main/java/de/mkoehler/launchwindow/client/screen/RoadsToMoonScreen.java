package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Concepts;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.MoonRoads;
import de.mkoehler.launchwindow.client.view.RoadsToMoon;
import de.mkoehler.launchwindow.client.view.Rockets;
import de.mkoehler.launchwindow.client.view.SetSkills;
import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Roads to the Moon ({@code ui-spec.md} &sect;4.21, from the canvas {@code Launch Window — Roads to the Moon},
 * 2026-10-26): the agency's roads to a Moon landing side by side, one column each, the same rows in every column so
 * they read across, with what each costs now, needs and asks of you, and the trade-offs in plain words. It never
 * ranks them: the only marks are "Your road" on a road the player has opened and a quiet "The historical choice".
 * Under the columns, the roads that reach the Moon without landing. Figures from {@link RoadsToMoon}, words from
 * {@link MoonRoads}.
 *
 * @author Mario Koehler
 */
public final class RoadsToMoonScreen extends ChromeScreen {

    /** The row labels' column, on the page. */
    private static final float LABEL_WIDTH = 112f;
    /** The gap between the columns. */
    private static final float GAP = Theme.SPACE_4;
    /** The width the grid fills: the page less its padding. */
    private static final float GRID_WIDTH = 1920f - 2 * Theme.SPACE_6;
    /** A cell's padding. */
    private static final float PAD = 10f;
    /** The hairline between rows: navy at 12 %. */
    private static final Color HAIRLINE = new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.12f);
    /** The label column's rules: white at 22 %. */
    private static final Color LABEL_RULE = new Color(1f, 1f, 1f, 0.22f);
    /** The locked veil: white at 72 %. */
    private static final Color VEIL = new Color(1f, 1f, 1f, 0.72f);

    private final List<HistoryNote> notes = new ArrayList<>();

    /**
     * Builds the screen from the session.
     *
     * @param game the game, in a session
     */
    public RoadsToMoonScreen(LaunchWindowGame game) {
        super(game, List.of(Crumb.SPACE_COMPLEX, Crumb.HEADQUARTERS, new Crumb("Roads to the Moon", null)),
            "Headquarters · Lunar expeditions", "Roads to the Moon");
        rebuild();
    }

    @Override
    protected void goBack() {
        game.setScreen(new BodyProgramsScreen(game, CelestialBody.MOON));
    }

    @Override
    protected Help.Context helpContext() {
        return Help.Context.ROADS_TO_THE_MOON;
    }

    /**
     * Pins a road's history note open, as a click on its icon does.
     *
     * @param index the road's column, from 0
     */
    public void pinHistoryNote(int index) {
        if (index < notes.size()) {
            notes.get(index).setPinned(true);
        }
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top().left();
        notes.clear();
        PlayerSnapshot state = session.state();
        MoonRoads words = MoonRoads.standard();
        List<RoadsToMoon.Road> roads = RoadsToMoon.roads(words, state, session.catalog());
        List<RoadsToMoon.NoLanding> others = RoadsToMoon.withoutLanding(words, state, session.catalog());

        Table page = new Table();
        page.top().left();
        Table intro = new Table();
        intro.left();
        intro.add(ui.label("Every road here lands on the Moon. They differ in what you build, which rocket you need "
            + "and how many flights it takes to get there.", Theme.TextStyle.BODY, Theme.ON_PAGE_STRONG)).left()
            .padRight(Theme.SPACE_3);
        Concepts.standard().byId("lunar-mission-modes").filter(Concepts.Concept::shown).ifPresent(concept ->
            intro.add(conceptChips(List.of(concept), () -> new RoadsToMoonScreen(game))).left());
        page.add(intro).left().padBottom(Theme.SPACE_3).row();
        if (roads.isEmpty()) {
            page.add(ui.label("No roads to the Moon for your agency in this game.", Theme.TextStyle.BODY,
                Theme.ON_PAGE_STRONG)).left().row();
        } else {
            page.add(grid(roads, state)).left().row();
        }
        if (!others.isEmpty()) {
            page.add(ui.label("Reach the Moon without landing".toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW,
                Theme.ON_PAGE_STRONG)).left().padTop(Theme.SPACE_5).padBottom(Theme.SPACE_2).row();
            Table strip = new Table();
            strip.left();
            for (RoadsToMoon.NoLanding other : others) {
                strip.add(stripCard(other, state)).width(560f).top().padRight(GAP);
            }
            page.add(strip).left().row();
        }
        page.add(ui.paragraph("Opening a road's Earth-orbit tests makes its Moon flights free to open. Rockets and "
                + "components you already own count for every road that uses them.", Theme.TextStyle.CAPTION,
            Theme.ON_PAGE_STRONG)).width(GRID_WIDTH).left().padTop(Theme.SPACE_3);
        content.add(keptScrollPane(page, "roads")).expand().fill();
    }

    // ---- The grid: a label column, then one column per road, the same rows in each

    private Table grid(List<RoadsToMoon.Road> roads, PlayerSnapshot state) {
        int count = roads.size();
        float width = (GRID_WIDTH - LABEL_WIDTH - count * GAP) / count;
        float inner = width - 2 * PAD;
        Table grid = new Table();
        grid.top().left();
        row(grid, null, roads, width, road -> head(road, inner), false);
        row(grid, "How it works", roads, width, road -> ui.paragraph(road.words().howItWorks(), Theme.TextStyle.BODY_SM,
            Theme.INK), true);
        row(grid, "To start now", roads, width, road -> start(road, state, inner), true);
        row(grid, "Hardware to develop", roads, width, road -> hardware(road, inner), true);
        row(grid, "The rocket it needs", roads, width, road -> rocket(road), true);
        row(grid, "Crew per flight", roads, width, road -> ui.label(road.crew() + " "
            + RoadsToMoon.crewWord(state.getArchetype(), road.crew()), Theme.TextStyle.BODY_SM, Theme.INK), true);
        row(grid, "Flights on the way", roads, width, road -> flights(road, inner), true);
        row(grid, "The landing", roads, width, road -> landing(road), true);
        row(grid, "Trade-offs", roads, width, road -> tradeOffs(road, inner), true);
        row(grid, null, roads, width, road -> foot(road, state), true);
        return grid;
    }

    /** One row: its label on the page, then a white cell per road; a hairline above it unless it's the first. */
    private void row(Table grid, String label, List<RoadsToMoon.Road> roads, float width,
                     Function<RoadsToMoon.Road, Actor> cell, boolean ruled) {
        if (ruled) {
            grid.add(new Image(ui.solid(label == null ? new Color(0, 0, 0, 0) : LABEL_RULE))).width(LABEL_WIDTH)
                .height(Theme.BORDER_THIN);
            for (int i = 0; i < roads.size(); i++) {
                Table rule = new Table();
                rule.setBackground(ui.solid(Theme.SURFACE_CARD));
                rule.add(new Image(ui.solid(HAIRLINE))).growX().height(Theme.BORDER_THIN);
                grid.add(rule).width(width).height(Theme.BORDER_THIN).padLeft(GAP);
            }
            grid.row();
        }
        Table name = new Table();
        name.top().left();
        if (label != null) {
            name.add(ui.paragraph(label.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.ON_PAGE_STRONG))
                .width(LABEL_WIDTH - Theme.SPACE_2).left().padTop(PAD);
        }
        grid.add(name).width(LABEL_WIDTH).fillY().top().left();
        for (RoadsToMoon.Road road : roads) {
            Table box = new Table();
            box.setBackground(ui.solid(Theme.SURFACE_CARD));
            box.top().left();
            box.pad(PAD);
            box.add(cell.apply(road)).width(width - 2 * PAD).left().top().expand();
            grid.add(box).width(width).fillY().padLeft(GAP);
        }
        grid.row();
    }

    private Table head(RoadsToMoon.Road road, float inner) {
        Table head = new Table();
        head.left();
        Drawable art = ui.croppedPicture("programs/" + road.moon().id() + ".jpg", 92f / 59f);
        head.add(art == null ? new Image(ui.solid(Theme.NAVY)) : new Image(art, Scaling.fill)).size(92f, 59f)
            .top().padRight(Theme.SPACE_3);
        Table text = new Table();
        text.top().left();
        text.add(ui.label(ContentNames.of(road.moon().id()), Theme.TextStyle.HEADING_SM, Theme.INK)).left()
            .padBottom(Theme.SPACE_2).row();
        Table tags = new Table();
        tags.left();
        if (road.yours()) {
            tags.add(ui.badge(Theme.BadgeKind.NAVY, "YOUR ROAD")).padRight(Theme.SPACE_2);
        }
        tags.add(modeBadge(road.words().mode())).padRight(Theme.SPACE_2);
        if (road.words().historical()) {
            Container<Label> quiet = new Container<>(ui.label("The historical choice", Theme.TextStyle.CAPTION_ITALIC,
                Theme.INK_MUTED));
            quiet.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            quiet.pad(2f, Theme.SPACE_2, 2f, Theme.SPACE_2);
            tags.add(quiet);
        }
        text.add(tags).left();
        head.add(text).expandX().left().top();
        return head;
    }

    /** The mode's badge: the outlined tag, with its glyph. */
    private Table modeBadge(MoonRoads.Mode mode) {
        Table badge = new Table();
        badge.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        badge.pad(3f, 6f, 3f, Theme.SPACE_2);
        Image glyph = new Image(ui.icon(mode.icon()), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        badge.add(glyph).size(12f).padRight(5f);
        badge.add(ui.label(mode.label().toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.NAVY));
        return badge;
    }

    private Actor start(RoadsToMoon.Road road, PlayerSnapshot state, float inner) {
        Table money = new Table();
        money.top().left();
        money.defaults().padBottom(2f);
        moneyLine(money, "Earth-orbit tests", road.tests(), road);
        moneyLine(money, "Moon flights", road.flights(), road);
        money.add(ui.label("Parts to build", Theme.TextStyle.BODY_SM, Theme.INK)).left().expandX();
        money.add(road.partsCost() == 0 ? ui.label("All built", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)
            : ui.label(money(road.partsCost()), Theme.TextStyle.BODY_SM, Theme.INK)).right().row();
        money.add(new Image(ui.solid(HAIRLINE))).colspan(2).growX().height(Theme.BORDER_THIN).padTop(4f)
            .padBottom(4f).row();
        money.add(ui.label("Total", Theme.TextStyle.BODY_STRONG, Theme.INK)).left();
        boolean short_ = road.total() > state.getFunds();
        money.add(ui.label(road.total() == 0 ? "Nothing to pay" : money(road.total()), Theme.TextStyle.BODY_STRONG,
            short_ ? Theme.DANGER : Theme.INK)).right().row();
        money.add(ui.label("Upkeep " + money(road.upkeep()) + " a season", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .colspan(2).left();
        Table capped = new Table();
        capped.left();
        capped.add(money).width(Math.min(inner, 460f)).left();
        if (road.missingGoals().isEmpty()) {
            return capped;
        }
        // Locked: the figures stay legible under a veil, with what opens the road (ui-spec.md 4.21, state 5).
        Stack stack = new Stack();
        stack.add(capped);
        Table veil = new Table();
        veil.setBackground(ui.solid(VEIL));
        Table framed = new Table();
        framed.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        framed.pad(6f, Theme.SPACE_3, 6f, Theme.SPACE_3);
        Image lock = new Image(ui.icon("lock"), Scaling.fit);
        lock.setColor(Theme.NAVY);
        framed.add(lock).size(14f).padRight(6f);
        framed.add(ui.label(opensWhen(road.missingGoals()), Theme.TextStyle.BODY_STRONG, Theme.NAVY));
        veil.add(framed);
        stack.add(veil);
        return stack;
    }

    private static String opensWhen(List<String> goals) {
        return "Opens when you reach " + String.join(" and ", goals.stream().map(ContentNames::of).toList());
    }

    private void moneyLine(Table money, String label, RoadsToMoon.Opening opening, RoadsToMoon.Road road) {
        money.add(ui.label(label, Theme.TextStyle.BODY_SM, Theme.INK)).left().expandX();
        Label value = switch (opening.cost()) {
            case OPEN -> ui.label("Open", Theme.TextStyle.BODY_SM, Theme.SUCCESS);
            case FREE -> ui.label(road.earthOrbit().id().equals(opening.freeBecause()) ? "Free: ready to open"
                : "Free: you run " + runs(opening.freeBecause()), Theme.TextStyle.BODY_SM, Theme.SUCCESS);
            case FREE_AFTER_TESTS -> ui.label("free once the tests are open", Theme.TextStyle.BODY_SM, Theme.INK_MUTED);
            case PRICED -> ui.label(money(opening.amount()), Theme.TextStyle.BODY_SM, Theme.INK);
        };
        money.add(value).right().row();
    }

    /** An open program as "Free: you run ..." names it: a road's tests as "Apollo's Earth-orbit tests". */
    private static String runs(String programId) {
        return MoonRoads.standard().roadOf(programId).filter(road -> road.earthOrbit().equals(programId))
            .map(road -> ContentNames.of(road.program()) + "'s Earth-orbit tests").orElse(ContentNames.of(programId));
    }

    private Table hardware(RoadsToMoon.Road road, float inner) {
        Table cell = new Table();
        cell.top().left();
        long left = road.toDevelop();
        cell.add(ui.label(left == 0 ? "All built" : left + " to develop", Theme.TextStyle.BODY_STRONG, Theme.INK))
            .left().padBottom(Theme.SPACE_2).row();
        HorizontalGroup chips = chipGroup();
        for (RoadsToMoon.Part part : road.parts()) {
            chips.addActor(partChip(part));
        }
        cell.add(chips).width(inner).left();
        return cell;
    }

    private HorizontalGroup chipGroup() {
        HorizontalGroup chips = new HorizontalGroup();
        chips.wrap();
        chips.rowLeft();
        chips.space(6f);
        chips.wrapSpace(6f);
        return chips;
    }

    private Table partChip(RoadsToMoon.Part part) {
        Table chip = new Table();
        chip.setBackground(ui.rect(Theme.SURFACE_CARD, part.owned() ? Theme.SUCCESS : Theme.NAVY_BORDER,
            Theme.BORDER_THIN));
        chip.pad(3f, 6f, 3f, Theme.SPACE_2);
        Image icon = new Image(ui.icon(SetSkills.icon(part.component().associatedSkill())), Scaling.fit);
        icon.setColor(Theme.NAVY);
        chip.add(icon).size(12f).padRight(5f);
        String name = ContentNames.of(part.component().id());
        if (part.owned()) {
            chip.add(ui.label(name + String.format(Locale.ROOT, " · %.0f %% · ", part.reliability()),
                Theme.TextStyle.CAPTION, Theme.INK));
            chip.add(ui.label("yours", Theme.TextStyle.CAPTION, Theme.SUCCESS));
            Image check = new Image(ui.icon("check"), Scaling.fit);
            check.setColor(Theme.SUCCESS);
            chip.add(check).size(11f).padLeft(3f);
        } else {
            chip.add(ui.label(name, Theme.TextStyle.CAPTION, Theme.INK));
        }
        return chip;
    }

    private Table rocket(RoadsToMoon.Road road) {
        Table cell = new Table();
        cell.top().left();
        RoadsToMoon.Rocket rocket = road.rocket();
        if (rocket == null) {
            cell.add(ui.label("None of your agency's rockets lifts its landing.", Theme.TextStyle.BODY_SM,
                Theme.DANGER)).left();
            return cell;
        }
        Drawable art = ui.croppedPicture("rockets/" + rocket.rocket().id() + ".jpg", 26f / 52f);
        cell.add(art == null ? new Image(ui.solid(Theme.NAVY_BORDER)) : new Image(art, Scaling.fill)).size(26f, 52f)
            .top().padRight(Theme.SPACE_3);
        Table text = new Table();
        text.top().left();
        Table name = new Table();
        name.add(ui.label(ContentNames.of(rocket.rocket().id()), Theme.TextStyle.BODY_STRONG, Theme.INK));
        name.add(ui.label(" · " + Rockets.className(rocket.rocket().rocket().rocketClass()), Theme.TextStyle.CAPTION,
            Theme.INK_MUTED));
        text.add(name).left().row();
        String second = rocket.launches() > 1 ? "Two launches per flight"
            : rocket.fromTheFirst() ? "A heavy rocket from the first test flight" : "Lifts the landing in one launch";
        text.add(ui.label(second, rocket.launches() > 1 ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY_SM,
            Theme.INK)).left().row();
        Label where = switch (rocket.state()) {
            case OPEN -> ui.label(String.format(Locale.ROOT, "Open · %.0f %%", rocket.reliability()),
                Theme.TextStyle.BODY_SM, Theme.SUCCESS);
            case NOT_OPEN -> ui.label("Not open: " + money(rocket.rocket().creationCost()) + " to open",
                Theme.TextStyle.BODY_SM, Theme.INK);
            case VAB_TOO_SMALL -> ui.label("Needs VAB level " + rocket.vabLevel(), Theme.TextStyle.BODY_SM,
                Theme.DANGER);
        };
        text.add(where).left();
        cell.add(text).expandX().left().top();
        return cell;
    }

    private Table flights(RoadsToMoon.Road road, float inner) {
        Table cell = new Table();
        cell.top().left();
        cell.add(ui.label(road.flightsOnWay() + (road.flightsOnWay() == 1 ? " flight" : " flights")
            + " before the landing", Theme.TextStyle.BODY_STRONG, Theme.INK)).left().padBottom(Theme.SPACE_2).row();
        HorizontalGroup chips = chipGroup();
        for (RoadsToMoon.First first : road.firsts()) {
            chips.addActor(goalChip(first));
        }
        cell.add(chips).width(inner).left();
        return cell;
    }

    private Table goalChip(RoadsToMoon.First first) {
        Table chip = new Table();
        chip.setBackground(first.reached()
            ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.05f), Theme.NAVY_BORDER, Theme.BORDER_THIN)
            : ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        chip.pad(3f, 6f, 3f, Theme.SPACE_2);
        Image icon = new Image(ui.icon(first.reached() ? "check" : "trophy"), Scaling.fit);
        icon.setColor(first.reached() ? Theme.INK_MUTED : Theme.NAVY);
        chip.add(icon).size(11f).padRight(5f);
        chip.add(ui.label(ContentNames.of(first.goalId()), Theme.TextStyle.CAPTION,
            first.reached() ? Theme.INK_MUTED : Theme.NAVY));
        return chip;
    }

    private Table landing(RoadsToMoon.Road road) {
        Table cell = new Table();
        cell.top().left();
        int steps = road.landingSteps();
        int risky = road.landingCrewRiskSteps();
        String riskText = risky == steps ? "every one with the crew at risk" : risky + " with the crew at risk";
        String launches = road.rocket() == null || road.rocket().launches() <= 1 ? "1 launch"
            : road.rocket().launches() + " launches, all must work";
        if (road.rocket() != null && road.rocket().launches() == 2) {
            launches = "2 launches, both must work";
        }
        Table line = new Table();
        line.left();
        line.add(ui.label(steps + " steps, " + riskText + " · ", Theme.TextStyle.BODY_SM, Theme.INK));
        line.add(ui.label(launches, Theme.TextStyle.BODY_STRONG, Theme.INK));
        cell.add(line).left().row();
        cell.add(ui.label(String.format(Locale.ROOT, "%,d", road.landing().firstTimePrestige()) + " prestige the first time",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left().padTop(2f);
        return cell;
    }

    private Table tradeOffs(RoadsToMoon.Road road, float inner) {
        Table cell = new Table();
        cell.top().left();
        float half = (inner - Theme.SPACE_4) / 2f;
        cell.add(bullets("For", road.words().pros(), Theme.SUCCESS, half)).width(half).top().left()
            .padRight(Theme.SPACE_4);
        cell.add(bullets("Against", road.words().cons(), Theme.GOLD, half)).width(half).top().left();
        return cell;
    }

    private Table bullets(String head, List<String> lines, Color dot, float width) {
        Table list = new Table();
        list.top().left();
        list.add(ui.label(head.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).colspan(2).left()
            .padBottom(4f).row();
        for (String line : lines) {
            list.add(new Image(ui.circle(dot, dot, 0f, 7))).size(7f).top().padTop(5f).padRight(6f);
            list.add(ui.paragraph(line, Theme.TextStyle.BODY_SM, Theme.INK)).width(width - 13f).left().padBottom(3f)
                .row();
        }
        return list;
    }

    private Table foot(RoadsToMoon.Road road, PlayerSnapshot state) {
        Table cell = new Table();
        HistoryNote note = new HistoryNote(ui, road.words().historyNote());
        notes.add(note);
        cell.add(note).size(HistoryNote.ICON_SIZE).left().expandX();
        Program next = road.next(state);
        String label = next.id().equals(road.moon().id()) ? "View the program" : "View the Earth-orbit tests";
        cell.add(actionButton(label, Theme.ButtonKind.SECONDARY, () -> game.setScreen(new ProgramDetailsScreen(game,
            next.id(), next.id().equals(road.moon().id()) ? CelestialBody.MOON : CelestialBody.EARTH)))).right();
        return cell;
    }

    // ---- The strip: the roads that reach the Moon without landing

    private Table stripCard(RoadsToMoon.NoLanding road, PlayerSnapshot state) {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        card.pad(Theme.SPACE_2);
        card.top().left();
        Drawable art = ui.croppedPicture("programs/" + road.moon().id() + ".jpg", 84f / 54f);
        card.add(art == null ? new Image(ui.solid(Theme.NAVY)) : new Image(art, Scaling.fill)).size(84f, 54f).top()
            .padRight(Theme.SPACE_3);
        Table text = new Table();
        text.top().left();
        Table head = new Table();
        head.add(ui.label(ContentNames.of(road.moon().id()), Theme.TextStyle.BODY_STRONG, Theme.INK));
        head.add(ui.label(" · " + road.words().reach().label(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX()
            .left();
        Label status = road.missingGoals().isEmpty()
            ? ui.label("To start now " + money(road.total()), Theme.TextStyle.CAPTION, road.total() > state.getFunds()
                ? Theme.DANGER : Theme.INK)
            : ui.label(opensWhen(road.missingGoals()), Theme.TextStyle.CAPTION, Theme.NAVY);
        // On the name's line when it fits; under it when the name and the status don't (large text, 2026-10-26).
        float room = 560f - 84f - Theme.SPACE_3 - 2 * Theme.SPACE_2;
        boolean fits = head.getPrefWidth() + status.getPrefWidth() + Theme.SPACE_3 <= room;
        if (fits) {
            head.add(status).right();
        }
        text.add(head).growX().padBottom(4f).row();
        if (!fits) {
            text.add(status).left().padBottom(4f).row();
        }
        HorizontalGroup chips = chipGroup();
        road.firsts().forEach(first -> chips.addActor(goalChip(first)));
        text.add(chips).width(560f - 84f - Theme.SPACE_3 - 2 * Theme.SPACE_2).left().padBottom(4f).row();
        Label line = ui.paragraph(road.words().line(), Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        line.setAlignment(Align.left);
        text.add(line).width(560f - 84f - Theme.SPACE_3 - 2 * Theme.SPACE_2).left();
        card.add(text).expandX().fillX().top();
        // The whole card leads to the program's page (Roads to the Moon canvas, state 6).
        card.setTouchable(Touchable.enabled);
        card.addListener(clicked(() -> game.setScreen(new ProgramDetailsScreen(game,
            state.getOpenedProgramIds().contains(road.earthOrbit().id()) ? road.moon().id() : road.earthOrbit().id(),
            state.getOpenedProgramIds().contains(road.earthOrbit().id()) ? CelestialBody.MOON : CelestialBody.EARTH))));
        return card;
    }
}
