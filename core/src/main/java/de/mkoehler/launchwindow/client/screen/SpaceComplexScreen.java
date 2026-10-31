package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Timer;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.ContractsView;
import de.mkoehler.launchwindow.client.view.DecisionView;
import de.mkoehler.launchwindow.client.view.Advisor;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

/**
 * The Space Complex hub ({@code ui-spec.md} &sect;4.3): the agency's art, full-bleed and without
 * the header and footer chrome, picked by archetype and game season, with the time of day
 * stepping on as the player comes and goes ({@link Hub.Clock}). Each building highlights on hover
 * with its name and opens its screen on click; a slim overlay strip across the top carries the
 * agency, the date, the countdown, the turn report and the funds and prestige.
 *
 * @author Mario Koehler
 */
public final class SpaceComplexScreen extends SessionScreen {

    /** The hover highlight: white at 18% for fill and outline alike, hard-edged (decided). */
    /** How long entering the game waits for the front pages before opening the report without them. */
    private static final float PAGES_WAIT_SECONDS = 4f;
    private static final Color HIGHLIGHT = new Color(1f, 1f, 1f, 0.18f);
    private static final Color LABEL_SHADOW = new Color(0f, 0f, 0f, 0.65f);
    private static final Color DIRECTOR = new Color(1f, 1f, 1f, 0.8f);
    /** Every strip chip's height. */
    private static final float CHIP_HEIGHT = 32f;
    /**
     * Every building's marker, a disc this far above its label (art pixels): white with the place's
     * icon, navy while hovered, and gold with a gear while upgrading. The markers show the player at
     * a glance what can be clicked (play-test request, 2026-10-05).
     */
    private static final float MARKER_RISE = 55f;
    /** The upgrading marker's radius. */
    private static final float MARKER_RADIUS = 22f;
    /** The plain marker's radius: a little smaller, so an upgrade still stands out. */
    private static final float IDLE_MARKER_RADIUS = 18f;
    /** The idle-people dot on a marker's upper right, in art pixels. */
    private static final float IDLE_DOT_RADIUS = 6f;
    /** The plain marker's icon size, in art pixels. */
    private static final float IDLE_MARKER_ICON = 20f;
    /** The most height a tall, narrow marker icon (the VAB's rocket) may use inside the disc. */
    private static final float TALL_MARKER_ICON = 26f;
    /** The advisor card's width. */
    private static final float ADVISOR_WIDTH = 380f;

    private final List<Hub.Hotspot> hotspots;
    private final Hub.TimeOfDay timeOfDay;
    private final Image art = new Image();
    private final HotspotLayer layer = new HotspotLayer();
    private final Table strip = new Table();
    /** The advisor's corner: bottom left, clear of every building's marker on both agencies' art. */
    private final Table advisorCorner = new Table();
    /** The press's card, top left under the strip, while it waits for a statement (ui-spec.md 4.18.3). */
    private final Table pressCorner = new Table();
    private boolean resolving;
    private final Label countdown;
    private final ShapeRenderer shapes = new ShapeRenderer();

    /**
     * Builds the hub from the session, stepping the time-of-day clock on entry.
     *
     * @param game the game, in a session
     */
    public SpaceComplexScreen(LaunchWindowGame game) {
        this(game, false);
    }

    /**
     * Builds the hub, opening an unread turn report at once when asked: on entering the game, not
     * on every return to the hub.
     *
     * @param game         the game, in a session
     * @param unreadReport whether to open an unread report
     */
    public SpaceComplexScreen(LaunchWindowGame game, boolean unreadReport) {
        this(game, unreadReport, false);
    }

    /**
     * Builds the hub; on entering the game, the unseen front pages come before the unread report
     * ({@code ui-spec.md} &sect;4.18).
     *
     * @param game         the game, in a session
     * @param unreadReport whether to open an unread report
     * @param pagesShown   whether the front pages were just shown, so they aren't fetched again
     */
    public SpaceComplexScreen(LaunchWindowGame game, boolean unreadReport, boolean pagesShown) {
        super(game);
        this.timeOfDay = game.hubClock().enter(System.currentTimeMillis());
        this.hotspots = loadHotspots();
        countdown = ui.label("", Theme.TextStyle.LABEL, Theme.NAVY);
        root.setBackground(ui.solid(Theme.NAVY));
        art.setScaling(Scaling.fill);

        Table overlay = new Table();
        overlay.top();
        overlay.add(strip).growX().row();
        overlay.add(pressCorner).left().pad(Theme.SPACE_6, Theme.SPACE_6, 0f, 0f).row();
        Stack stack = new Stack();
        stack.add(art);
        stack.add(layer);
        stack.add(overlay);
        advisorCorner.bottom().left().pad(0f, Theme.SPACE_6, Theme.SPACE_6, 0f);
        stack.add(advisorCorner);
        root.add(stack).grow();
        rebuild();
        if (unreadReport && !pagesShown) {
            newTurn();
        } else {
            afterPages(unreadReport);
        }
    }

    /**
     * Brings in an unread turn the spoiler-free way, wherever it arrives from: entering the game, the
     * report chip or the dev button (2026-10-27). Missions flown? Their replays come first, or the offer
     * of them, before the pages and the report give the endings away ({@code ui-spec.md} &sect;4.4).
     */
    private void newTurn() {
        if (!game.reportUnread() || game.replaysFirst() == LaunchWindowGame.ReplaysFirst.NEVER) {
            pagesThenReport();
            return;
        }
        fetchLatestReport(report -> {
            if (report.missions().isEmpty()) {
                pagesThenReport();
            } else if (game.replaysFirst() == LaunchWindowGame.ReplaysFirst.ALWAYS) {
                watchReplays(report);
            } else {
                offerReplays(report);
            }
        }, this::pagesThenReport);
    }

    /**
     * Opens the latest report from the hub: an unread one the spoiler-free way ({@link #newTurn}), a
     * read one at once.
     */
    private void openReportFromHub() {
        if (game.reportUnread()) {
            newTurn();
        } else {
            openLatestReport(null);
        }
    }

    /**
     * Shows the unseen front pages, then the unread report, as entering the game did before the
     * replays came first.
     */
    private void pagesThenReport() {
        // The pages come first, but a slow server mustn't hold up the report: after a few
        // seconds without an answer the report opens, and a late answer is dropped.
        boolean[] settled = {false};
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (!settled[0] && isShowing()) {
                    settled[0] = true;
                    afterPages(true);
                }
            }
        }, PAGES_WAIT_SECONDS);
        fetchFrontPages(() -> {
            if (settled[0]) {
                return;
            }
            settled[0] = true;
            List<FrontPage> unseen = FrontPageScreen.unseen(game);
            if (unseen.isEmpty()) {
                // The fetch brought the losses the press waits on (ui-spec.md 4.18.3): show its card now.
                rebuild();
                afterPages(true);
            } else {
                game.setScreen(new FrontPageScreen(game, unseen,
                    () -> game.setScreen(new SpaceComplexScreen(game, true, true)), false));
            }
        });
    }

    /**
     * Asks whether to watch a new turn's missions before its report ({@code ui-spec.md} &sect;4.4).
     *
     * @param report the unread report, with missions flown
     */
    public void offerReplays(TurnReport report) {
        int count = report.missions().size();
        Table card = dialogCard("Last night", count == 1 ? "Your mission has flown. Watch it first, spoiler-free?"
            : "Your " + count + " missions have flown. Watch them first, spoiler-free?");
        card.add(ui.paragraph("The turn report tells how " + (count == 1 ? "it" : "they") + " went. The replay shows "
            + "it as it happened, step by step, and the report comes after.", Theme.TextStyle.BODY, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_4).row();
        boolean[] dontAsk = {false};
        Table box = new Table();
        Table toggle = new Table();
        toggle.left();
        Runnable draw = () -> {
            box.clearChildren();
            box.setBackground(dontAsk[0] ? ui.solid(Theme.NAVY) : ui.rect(Theme.SURFACE_CARD, Theme.NAVY,
                Theme.BORDER_EMPHASIS));
            if (dontAsk[0]) {
                Image check = new Image(ui.icon("check"), Scaling.fit);
                check.setColor(Theme.ON_FILL);
                box.add(check).size(12f);
            }
        };
        draw.run();
        toggle.add(box).size(18f).padRight(Theme.SPACE_2);
        toggle.add(ui.label("Don't ask again (Settings can change it)", Theme.TextStyle.BODY_SM, Theme.INK));
        toggle.setTouchable(Touchable.enabled);
        toggle.addListener(clicked(() -> {
            dontAsk[0] = !dontAsk[0];
            draw.run();
        }));
        card.add(toggle).left().padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(ChromeScreen.onClick(ui.button("Show the report", Theme.ButtonKind.SECONDARY), () -> {
            closeDialog();
            if (dontAsk[0]) {
                game.setReplaysFirst(LaunchWindowGame.ReplaysFirst.NEVER);
            }
            pagesThenReport();
        })).padRight(10f);
        buttons.add(ChromeScreen.onClick(ui.button(count == 1 ? "Watch the mission" : "Watch the missions",
            Theme.ButtonKind.PRIMARY), () -> {
                closeDialog();
                if (dontAsk[0]) {
                    game.setReplaysFirst(LaunchWindowGame.ReplaysFirst.ALWAYS);
                }
                watchReplays(report);
            }));
        card.add(buttons).right().fill(false, false);
        showDialog(card);
    }

    private void watchReplays(TurnReport report) {
        List<String> missions = report.missions().stream().map(TurnReport.MissionFlown::missionInstanceId).toList();
        game.setScreen(new ReplayScreen(game, report, missions.get(0), missions));
    }

    private void afterPages(boolean unreadReport) {
        if (unreadReport && game.reportUnread()) {
            openUnreadReport();
        } else if (game.resultUnseen()) {
            // The first hub visit after the result announces it, until the player closes it.
            showResult();
        }
    }

    /**
     * Asks the dev-mode server to resolve the open turn now, then brings the turn in as a new one does:
     * replays first, the pages, the report.
     */
    private void resolveTurnNow() {
        if (resolving) {
            return;
        }
        resolving = true;
        rebuildStrip(session.state());
        game.gateway().resolveTurnNow(session.credentials(), result -> {
            resolving = false;
            if (!isShowing()) {
                return;
            }
            if (result.kind() != ServerResult.Kind.OK) {
                rebuildStrip(session.state());
                toast("DEV_MODE_REQUIRED".equals(result.message()) ? "The server isn't in dev mode any more."
                    : "Couldn't resolve the turn: " + result.message(), Theme.NoticeKind.DANGER);
                return;
            }
            if (result.state() != null) {
                session.update(result);
            }
            stateChanged();
            newTurn();
        });
    }

    /**
     * Opens the game result dialog over the hub.
     */
    public void showResult() {
        new GameResultDialog(this).open();
    }

    private List<Hub.Hotspot> loadHotspots() {
        try (InputStream in = Gdx.files.internal(Hub.hotspotPath(session.state().getArchetype())).read()) {
            return Hub.readHotspots(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void rebuild() {
        PlayerSnapshot state = session.state();
        // The season can change while the player sits here (a turn resolved); the time of day can't.
        Drawable picture = ui.backdrop(Hub.artPath(state.getArchetype(), session.gameInfo().response().date().season(),
            timeOfDay));
        art.setDrawable(picture);
        rebuildStrip(state);
        rebuildAdvisor(state);
        pressCorner.clearChildren();
        PressCards.waiting(this).ifPresent(loss -> pressCorner.add(PressCards.own(this, loss, true, this::rebuild))
            .width(420f));
    }

    /**
     * Builds the advisor's card ({@code ui-spec.md} component 24): the next things a new agency
     * should do, then the road ahead, until there are none left, when it retires for good. It can be
     * minimized but never hidden (user, 2026-10-26).
     */
    private void rebuildAdvisor(PlayerSnapshot state) {
        advisorCorner.clearChildren();
        // Nothing to advise on once the game is decided.
        if (session.gameInfo().response().decided()) {
            return;
        }
        boolean tip = idleTip(state);
        if (game.advisorRetired() && !tip) {
            return;
        }
        Advisor.Marks marks = new Advisor.Marks(game.helpOpened(), session.latestResolvedTurn() >= 1,
            game.advisorReportRead());
        if (!game.advisorRetired() && Advisor.finished(state, session.catalog(), marks)) {
            game.retireAdvisor();
            if (!tip) {
                return;
            }
        }
        // Retired, the card comes back for the idle tip alone.
        boolean tipOnly = game.advisorRetired();
        List<Advisor.Item> open = new java.util.ArrayList<>(tipOnly ? List.of()
            : Advisor.open(state, session.catalog(), marks));
        if (tip) {
            open.add(0, Advisor.IDLE_TIP);
        }
        float inner = ADVISOR_WIDTH - 2 * Theme.SPACE_4;

        Table card = new Table();
        // Touchable itself, so the hotspots underneath never take a click through its gaps.
        card.setTouchable(Touchable.enabled);
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.top().left();
        boolean minimized = game.advisorMinimized();
        Table head = new Table();
        head.setBackground(ui.solid(Theme.NAVY));
        head.pad(8f, Theme.SPACE_4, 8f, Theme.SPACE_4);
        head.add(ui.label("ADVISOR", Theme.TextStyle.EYEBROW, Theme.ON_FILL)).left().expandX();
        // Minimized, the head still says how much is left.
        String caption = tipOnly ? "A tip" : !minimized
            ? (Advisor.firstStepsDone(state, session.catalog(), marks) ? "The road ahead" : "Your first seasons")
            : open.isEmpty() ? "All set for now" : open.size() + (open.size() == 1 ? " thing to do" : " things to do");
        head.add(ui.label(caption, Theme.TextStyle.CAPTION, DIRECTOR)).right().padRight(Theme.SPACE_2);
        // The chevron points the way the card goes: down to minimize, up to open again. The whole
        // head toggles it, the chevron being only the hint.
        Image chevron = new Image(ui.icon("chevron"), Scaling.fit);
        chevron.setColor(Theme.ON_FILL);
        chevron.setOrigin(6f, 6f);
        chevron.setRotation(minimized ? 90f : -90f);
        head.add(chevron).size(12f);
        head.setTouchable(Touchable.enabled);
        head.addListener(clicked(() -> {
            game.setAdvisorMinimized(!game.advisorMinimized());
            rebuildAdvisor(session.state());
        }));
        card.add(head).growX().row();
        if (minimized) {
            advisorCorner.add(card).width(ADVISOR_WIDTH);
            return;
        }

        Table body = new Table();
        body.top().left();
        body.pad(Theme.SPACE_4, Theme.SPACE_4, Theme.SPACE_3, Theme.SPACE_4);
        if (open.isEmpty()) {
            Advisor.Item next = Advisor.later(state, session.catalog(), marks).get(0);
            body.add(ui.label("All set for now", Theme.TextStyle.BODY_STRONG, Theme.INK)).left().row();
            String title = Advisor.title(next, state, session.catalog());
            body.add(ui.paragraph(Advisor.waitingFor(next, state) + ": " + Character.toLowerCase(title.charAt(0))
                + title.substring(1) + ".", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).width(inner).left()
                .padBottom(Theme.SPACE_3).row();
        } else {
            for (int i = 0; i < Math.min(Advisor.SHOWN, open.size()); i++) {
                body.add(advisorItem(open.get(i), inner)).width(inner).left().padBottom(Theme.SPACE_3).row();
            }
            if (open.size() > Advisor.SHOWN) {
                int more = open.size() - Advisor.SHOWN;
                body.add(ui.label(more + (more == 1 ? " more thing" : " more things") + " after these.",
                    Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left().padBottom(Theme.SPACE_3).row();
            }
        }
        if (tipOnly) {
            card.add(body).width(ADVISOR_WIDTH).row();
            advisorCorner.add(card).width(ADVISOR_WIDTH);
            return;
        }
        Table rule = new Table();
        rule.setBackground(ui.solid(Theme.NAVY_BORDER));
        body.add(rule).width(inner).height(1f).padBottom(Theme.SPACE_3).row();
        body.add(ui.paragraph("There's no button to end a turn: your orders stand, and the turn resolves on its own "
            + "when the countdown runs out.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).width(inner).left()
            .padBottom(Theme.SPACE_2).row();
        card.add(body).width(ADVISOR_WIDTH).row();
        advisorCorner.add(card).width(ADVISOR_WIDTH);
    }

    /**
     * Whether the advisor's idle tip shows now: the first time someone sits idle after the first
     * steps, until the player sends someone to training, nobody is idle any more, or they dismiss
     * it; then never again.
     */
    private boolean idleTip(PlayerSnapshot state) {
        if (game.idleTipDone()) {
            return false;
        }
        if (Advisor.idleTipDue(state)) {
            game.markIdleTipShown();
            return true;
        }
        if (game.idleTipShown()) {
            game.markIdleTipDone();
        }
        return false;
    }

    private Table advisorItem(Advisor.Item item, float width) {
        Table row = new Table();
        row.top().left();
        Table text = new Table();
        text.top().left();
        text.add(ui.label(Advisor.title(item, session.state(), session.catalog()), Theme.TextStyle.BODY_STRONG,
            Theme.INK)).left().row();
        text.add(ui.paragraph(Advisor.text(item, session.state(), session.catalog()), Theme.TextStyle.BODY_SM,
            Theme.INK_MUTED)).width(width - 4f).left()
            .padBottom(Theme.SPACE_1).row();
        String progress = Advisor.progress(item, session.state(), session.catalog());
        if (progress != null) {
            text.add(ui.label(progress, Theme.TextStyle.BODY_SM, Theme.NAVY)).left().padBottom(Theme.SPACE_1).row();
        }
        Table links = new Table();
        if (item.destination() != Advisor.Destination.HELP) {
            Label go = ui.label(item.go().toUpperCase(java.util.Locale.ROOT) + "  ›", Theme.TextStyle.LABEL,
                Theme.NAVY);
            go.setTouchable(Touchable.enabled);
            go.addListener(clicked(() -> go(Advisor.destination(item, session.state(), session.catalog()))));
            links.add(go).padRight(Theme.SPACE_4);
        }
        Table rules = new Table();
        rules.setTouchable(Touchable.enabled);
        Image glyph = new Image(ui.icon("help"), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        rules.add(glyph).size(12f).padRight(4f);
        rules.add(ui.label(item.destination() == Advisor.Destination.HELP ? "OPEN HELP" : "RULES",
            Theme.TextStyle.LABEL, Theme.NAVY));
        // The item's own topics, the chip naming the item ("2 topics for "Hire engineers"").
        rules.addListener(clicked(() -> openHelp(advisorTopics(item,
            Advisor.title(item, session.state(), session.catalog())))));
        links.add(rules);
        // The idle tip is a one-off: it can be put away without acting on it.
        if (item == Advisor.IDLE_TIP) {
            Label gotIt = ui.label("GOT IT", Theme.TextStyle.LABEL, Theme.NAVY);
            gotIt.setTouchable(Touchable.enabled);
            gotIt.addListener(clicked(() -> {
                game.markIdleTipDone();
                rebuildAdvisor(session.state());
            }));
            links.add(gotIt).padLeft(Theme.SPACE_4);
        }
        text.add(links).left();
        row.add(text).width(width).left();
        return row;
    }

    @Override
    public void closeHelp() {
        super.closeHelp();
        // Opening help was one of the advisor's items.
        rebuildAdvisor(session.state());
    }

    /**
     * Returns the help topics an advisor item's "Rules" link opens on.
     *
     * @param item the item
     * @return its topics, named after it
     */
    public static Help.Topics advisorTopics(Advisor.Item item) {
        return advisorTopics(item, item.title());
    }

    private static Help.Topics advisorTopics(Advisor.Item item, String title) {
        return new Help.Topics("\"" + title + "\"", item.topicIds());
    }

    private void go(Advisor.Destination destination) {
        switch (destination) {
            case HELP -> openHelp();
            case SET_CENTRE -> goSetCentre();
            case HEADQUARTERS -> goHeadquarters();
            case VAB -> goVab();
            case RND -> game.setScreen(new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD));
            case ROCKET_RND -> game.setScreen(new RndBoardScreen(game, RndBoardScreen.Board.ROCKET));
            case MISSION_CONTROL -> goMissionControl();
            case ASTRONAUT_COMPLEX -> goAstronautComplex();
            case TURN_REPORT -> openReportFromHub();
            case ROADS_TO_THE_MOON -> game.setScreen(new RoadsToMoonScreen(game));
        }
    }

    private void rebuildStrip(PlayerSnapshot state) {
        // No fade and nothing see-through: the art runs from near-black (winter night) to near-white
        // (Soviet winter noon), so everything on it sits on a solid surface.
        strip.clearChildren();
        strip.pad(20f, Theme.SPACE_6, 20f, Theme.SPACE_6);

        Table agency = new Table();
        agency.setBackground(ui.solid(Theme.NAVY));
        agency.pad(8f, 14f, 8f, 14f);
        agency.defaults().left();
        agency.add(ui.label(state.getAgencyName(), Theme.TextStyle.HEADING_SM, Theme.ON_FILL)).padBottom(2f).row();
        agency.add(ui.label("Dir. " + state.getDirectorName(), Theme.TextStyle.CAPTION, DIRECTOR));
        Table left = new Table();
        left.top().left();
        left.add(agency).left().row();
        if (session.packStatus() == ClientSession.PackStatus.DIFFERENT) {
            left.add(ui.notice(Theme.NoticeKind.WARNING, "Content differs.", "Your copy of this game's content "
                + "differs from the server's. Some numbers shown may be off until the client can download the "
                + "game's own copy.")).width(480f).left().padTop(Theme.SPACE_3);
        }
        strip.add(left).expandX().left().top();

        Table chips = new Table();
        float gap = 10f;
        if (session.gameInfo().response().devMode()) {
            // Play testing only: a dev-mode server resolves the open turn on demand.
            Table resolve = chip(ui.rect(Theme.GOLD, Theme.NAVY, Theme.BORDER_EMPHASIS), "clock", Theme.NAVY,
                ui.label(resolving ? "RESOLVING…" : "DEV · RESOLVE TURN NOW", Theme.TextStyle.LABEL, Theme.NAVY));
            resolve.setTouchable(Touchable.enabled);
            resolve.addListener(clicked(this::resolveTurnNow));
            chips.add(resolve).height(CHIP_HEIGHT).padRight(gap * 2);
        }
        chips.add(chip(ui.solid(Theme.NAVY), "game_time", Theme.ON_FILL,
            ui.label(session.gameInfo().response().date().label().toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                Theme.ON_FILL))).height(CHIP_HEIGHT).padRight(gap);
        chips.add(chip(ui.solid(Theme.SURFACE_CARD), "clock", Theme.NAVY, countdown)).height(CHIP_HEIGHT)
            .padRight(gap);
        // The strip's one button: white like its neighbours, with a navy outline.
        Table report = chip(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS), "document", Theme.NAVY,
            ui.label("TURN REPORT", Theme.TextStyle.LABEL, Theme.NAVY));
        if (game.reportUnread()) {
            // The unread mark: a gold dot, as in the mockup.
            report.add(new Image(ui.unreadDot())).size(10f).padLeft(6f);
        }
        report.setTouchable(Touchable.enabled);
        report.addListener(clicked(this::openReportFromHub));
        chips.add(report).height(CHIP_HEIGHT).padRight(gap);
        chips.add(chip(ui.solid(Theme.SURFACE_CARD), "funds", Theme.NAVY,
            ui.label(money(state.getFunds()), Theme.TextStyle.LABEL, Theme.NAVY))).height(CHIP_HEIGHT).padRight(gap);
        chips.add(chip(ui.solid(Theme.SURFACE_CARD), "prestige", Theme.NAVY,
            ui.label(String.format(Locale.ROOT, "%,d", state.getPrestige()), Theme.TextStyle.LABEL, Theme.NAVY)))
            .height(CHIP_HEIGHT).padRight(gap);
        // Settings, the hub having no header (decided 2026-10-04): a white icon-only button, like IconButton.
        Table gear = new Table();
        gear.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN));
        Image glyph = new Image(ui.icon("settings"), Scaling.fit);
        glyph.setColor(Theme.NAVY);
        gear.add(glyph).size(16f);
        gear.setTouchable(Touchable.enabled);
        gear.addListener(clicked(this::openSettings));
        chips.add(gear).size(CHIP_HEIGHT).padRight(gap);
        // Help, beside Settings as in every header (ui-spec.md component 23).
        Table helpButton = new Table();
        helpButton.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.ON_PAGE, Theme.BORDER_THIN));
        Image helpGlyph = new Image(ui.icon("help"), Scaling.fit);
        helpGlyph.setColor(Theme.NAVY);
        helpButton.add(helpGlyph).size(16f);
        helpButton.setTouchable(Touchable.enabled);
        helpButton.addListener(clicked(this::openHelp));
        ui.tooltip(helpButton, Tips.help().title(), Tips.help().text());
        chips.add(helpButton).size(CHIP_HEIGHT);
        strip.add(chips).right().top();
    }

    private Table chip(Drawable background, String icon, Color color, Label text) {
        Table chip = new Table();
        chip.setBackground(background);
        chip.pad(0f, 12f, 0f, 12f);
        Image glyph = new Image(ui.icon(icon), Scaling.fit);
        glyph.setColor(color);
        chip.add(glyph).size(14f).padRight(6f);
        chip.add(text);
        return chip;
    }

    private static String money(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    @Override
    public void render(float delta) {
        countdown.setText(nextTurnText());
        super.render(delta);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        super.dispose();
    }

    private void open(Hub.Place place) {
        switch (place) {
            case HEADQUARTERS -> goHeadquarters();
            case SET_CENTRE -> goSetCentre();
            case VAB -> goVab();
            case MISSION_CONTROL -> goMissionControl();
            case MUSEUM -> goMuseum();
            case ASTRONAUT_COMPLEX -> goAstronautComplex();
            case PUBLIC_AFFAIRS_OFFICE -> goPublicAffairs();
            case INTELLIGENCE_OFFICE -> goIntelligenceOffice();
            default -> notBuiltYet(place.inSentence());
        }
    }

    /**
     * Shows a building as hovered, as the pointer resting on it does. For the offscreen screenshot
     * tool.
     *
     * @param place the place, or {@code null} for none
     */
    public void hover(Hub.Place place) {
        layer.hovered = place;
    }

    /**
     * The hotspots: finds the building under the pointer, draws its highlight and name, opens it on
     * click, and marks the buildings that are upgrading.
     */
    private final class HotspotLayer extends Actor {
        private final Label name = ui.label("", Theme.TextStyle.HEADING_SM, Theme.ON_FILL);
        private final Label shadow = ui.label("", Theme.TextStyle.HEADING_SM, LABEL_SHADOW);
        private final Label idleName = ui.label("", Theme.TextStyle.BODY_STRONG, Theme.ON_FILL);
        private final Label idleShadow = ui.label("", Theme.TextStyle.BODY_STRONG, LABEL_SHADOW);
        private final Drawable gear = ui.icon("settings");
        private final java.util.Map<Hub.Place, Drawable> markerIcons = new java.util.EnumMap<>(Hub.Place.class);
        private Hub.Place hovered;

        HotspotLayer() {
            setTouchable(Touchable.enabled);
            addListener(new InputListener() {
                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    hovered = placeAt(x, y);
                    return false;
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (pointer == -1) {
                        hovered = null;
                    }
                }
            });
            addListener(clicked(() -> {
                if (hovered != null) {
                    open(hovered);
                }
            }));
        }

        /** Converts a local point to art pixels; the layer covers the whole art. */
        private float artX(float x) {
            return x * Hub.ART_WIDTH / getWidth();
        }

        private float artY(float y) {
            return y * Hub.ART_HEIGHT / getHeight();
        }

        private Hub.Place placeAt(float x, float y) {
            float ax = artX(x);
            float ay = artY(y);
            for (Hub.Hotspot spot : hotspots) {
                // The marker counts as the building: it's what draws the pointer there.
                float dx = ax - spot.labelX();
                float dy = ay - (spot.labelY() + MARKER_RISE);
                if (spot.contains(ax, ay) || dx * dx + dy * dy <= MARKER_RADIUS * MARKER_RADIUS) {
                    return spot.place();
                }
            }
            return null;
        }

        private Drawable markerIcon(Hub.Place place) {
            return markerIcons.computeIfAbsent(place, p -> ui.icon(p.icon()));
        }

        @Override
        public Actor hit(float x, float y, boolean touchable) {
            if (touchable && getTouchable() != Touchable.enabled) {
                return null;
            }
            // Only the buildings take the pointer, so a click on bare ground does nothing.
            return placeAt(x, y) != null ? this : null;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float scaleX = getWidth() / Hub.ART_WIDTH;
            float scaleY = getHeight() / Hub.ART_HEIGHT;
            List<Hub.Hotspot> upgrading = hotspots.stream().filter(spot -> isUpgrading(spot.place())).toList();
            List<Hub.Hotspot> plain = hotspots.stream().filter(spot -> !isUpgrading(spot.place())).toList();
            Hub.Hotspot hover = hotspots.stream().filter(spot -> spot.place() == hovered).findFirst().orElse(null);
            batch.end();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.setProjectionMatrix(batch.getProjectionMatrix());
            shapes.setTransformMatrix(batch.getTransformMatrix());
            shapes.translate(getX(), getY(), 0f);
            shapes.scale(scaleX, scaleY, 1f);
            if (hover != null) {
                float[] v = hover.vertices();
                short[] tri = hover.triangles();
                shapes.begin(ShapeRenderer.ShapeType.Filled);
                shapes.setColor(HIGHLIGHT);
                for (int i = 0; i < tri.length; i += 3) {
                    shapes.triangle(v[2 * tri[i]], v[2 * tri[i] + 1], v[2 * tri[i + 1]], v[2 * tri[i + 1] + 1],
                        v[2 * tri[i + 2]], v[2 * tri[i + 2] + 1]);
                }
                shapes.end();
                Gdx.gl.glLineWidth(2f);
                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(HIGHLIGHT);
                shapes.polygon(v);
                shapes.end();
                Gdx.gl.glLineWidth(1f);
            }
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            for (Hub.Hotspot spot : plain) {
                boolean hot = spot.place() == hovered;
                shapes.setColor(hot ? Color.WHITE : Theme.NAVY);
                shapes.circle(spot.labelX(), spot.labelY() + MARKER_RISE, IDLE_MARKER_RADIUS + 1.5f, 40);
                shapes.setColor(hot ? Theme.NAVY : Color.WHITE);
                shapes.circle(spot.labelX(), spot.labelY() + MARKER_RISE, IDLE_MARKER_RADIUS, 40);
            }
            shapes.end();
            if (!upgrading.isEmpty()) {
                shapes.begin(ShapeRenderer.ShapeType.Filled);
                for (Hub.Hotspot spot : upgrading) {
                    shapes.setColor(Color.WHITE);
                    shapes.circle(spot.labelX(), spot.labelY() + MARKER_RISE, MARKER_RADIUS + 2f, 48);
                    shapes.setColor(Theme.GOLD);
                    shapes.circle(spot.labelX(), spot.labelY() + MARKER_RISE, MARKER_RADIUS, 48);
                }
                shapes.end();
            }
            // Idle people (the user's idea, 2026-10-10): a red dot on the marker's upper right.
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            for (Hub.Hotspot spot : hotspots) {
                if (Hub.idle(spot.place(), session.state()) == 0) {
                    continue;
                }
                float radius = isUpgrading(spot.place()) ? MARKER_RADIUS : IDLE_MARKER_RADIUS;
                float dotX = spot.labelX() + radius * 0.72f;
                float dotY = spot.labelY() + MARKER_RISE + radius * 0.72f;
                shapes.setColor(Color.WHITE);
                shapes.circle(dotX, dotY, IDLE_DOT_RADIUS + 1.5f, 24);
                shapes.setColor(Theme.DANGER);
                shapes.circle(dotX, dotY, IDLE_DOT_RADIUS, 24);
            }
            // A decision waiting (ui-spec.md component 26): the same dot in gold, upper left. Headquarters
            // gets it for new contracts on offer and for a contract still to launch (ui-spec.md section 4.17).
            Hub.Place deciding = decisionPlace();
            boolean offers = newContracts() || contractToLaunch() != null;
            for (Hub.Hotspot spot : hotspots) {
                if (spot.place() != deciding && !(offers && spot.place() == Hub.Place.HEADQUARTERS)) {
                    continue;
                }
                float radius = isUpgrading(spot.place()) ? MARKER_RADIUS : IDLE_MARKER_RADIUS;
                float dotX = spot.labelX() - radius * 0.72f;
                float dotY = spot.labelY() + MARKER_RISE + radius * 0.72f;
                shapes.setColor(Color.WHITE);
                shapes.circle(dotX, dotY, IDLE_DOT_RADIUS + 1.5f, 24);
                shapes.setColor(Theme.GOLD);
                shapes.circle(dotX, dotY, IDLE_DOT_RADIUS, 24);
            }
            shapes.end();
            shapes.identity();
            batch.begin();
            for (Hub.Hotspot spot : plain) {
                Color tint = spot.place() == hovered ? Color.WHITE : Theme.NAVY;
                batch.setColor(tint.r, tint.g, tint.b, parentAlpha);
                Drawable icon = markerIcon(spot.place());
                // Keep the icon's own proportions: the rocket is tall and narrow, and a square stretched it.
                float aspect = ui.iconAspect(spot.place().icon());
                float h = aspect < 1f ? Math.min(TALL_MARKER_ICON, IDLE_MARKER_ICON / aspect) : IDLE_MARKER_ICON / aspect;
                float w = h * aspect;
                icon.draw(batch, getX() + (spot.labelX() - w / 2f) * scaleX,
                    getY() + (spot.labelY() + MARKER_RISE - h / 2f) * scaleY, w * scaleX, h * scaleY);
            }
            for (Hub.Hotspot spot : upgrading) {
                batch.setColor(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, parentAlpha);
                gear.draw(batch, getX() + (spot.labelX() - 12f) * scaleX, getY() + (spot.labelY() + MARKER_RISE - 12f)
                    * scaleY, 24f * scaleX, 24f * scaleY);
            }
            batch.setColor(1f, 1f, 1f, parentAlpha);
            if (hover != null) {
                String text = hover.place().label().toUpperCase(Locale.ROOT);
                drawCentered(batch, shadow, text, hover.labelX() * scaleX + 1.5f, hover.labelY() * scaleY - 1.5f,
                    parentAlpha);
                drawCentered(batch, name, text, hover.labelX() * scaleX, hover.labelY() * scaleY, parentAlpha);
                float below = hover.labelY() * scaleY - name.getHeight() - 4f;
                String idle = Hub.idleLine(hover.place(), session.state());
                if (idle != null) {
                    drawCentered(batch, idleShadow, idle, hover.labelX() * scaleX + 1.5f, below - 1.5f, parentAlpha);
                    drawCentered(batch, idleName, idle, hover.labelX() * scaleX, below, parentAlpha);
                    below -= idleName.getHeight() + 2f;
                }
                if (hover.place() == decisionPlace()) {
                    String waiting = (session.state().getPendingDecision().handsOnInProgress()
                        ? "Hands-on in progress: " : "A decision waiting: ")
                        + ContentNames.of(session.state().getPendingDecision().componentId());
                    drawCentered(batch, idleShadow, waiting, hover.labelX() * scaleX + 1.5f, below - 1.5f,
                        parentAlpha);
                    drawCentered(batch, idleName, waiting, hover.labelX() * scaleX, below, parentAlpha);
                    below -= idleName.getHeight() + 2f;
                }
                String contracts = hover.place() != Hub.Place.HEADQUARTERS ? null
                    : newContracts() ? "New contracts on offer" : contractToLaunch();
                if (contracts != null) {
                    String offered = contracts;
                    drawCentered(batch, idleShadow, offered, hover.labelX() * scaleX + 1.5f, below - 1.5f,
                        parentAlpha);
                    drawCentered(batch, idleName, offered, hover.labelX() * scaleX, below, parentAlpha);
                }
            }
        }

        private void drawCentered(Batch batch, Label label, String text, float x, float y, float parentAlpha) {
            label.setText(text);
            label.setAlignment(Align.center);
            label.pack();
            label.setPosition(getX() + x - label.getWidth() / 2f, getY() + y - label.getHeight() / 2f);
            label.draw(batch, parentAlpha);
        }

        /** Whether a contract the agency can apply for is new this turn, and it hasn't applied anywhere. */
        private boolean newContracts() {
            return ContractsView.newOffers(session.state(), session.catalog(), session.openTurn());
        }

        /** The reminder of an awarded contract with no mission launched for it, or {@code null}. */
        private String contractToLaunch() {
            return ContractsView.launchReminder(session.state(), session.openTurn());
        }

        /** The building whose board holds a waiting decision: the VAB for a rocket, else the SET Centre. */
        private Hub.Place decisionPlace() {
            if (!DecisionView.waiting(session.state())) {
                return null;
            }
            return DecisionView.rocket(session.state().getPendingDecision()
                .componentId(), session.catalog()) ? Hub.Place.VAB : Hub.Place.SET_CENTRE;
        }

        private boolean isUpgrading(Hub.Place place) {
            Building building = place.building();
            return building != null && session.state().getBuildingUpgradeSeasonsRemaining().containsKey(building);
        }
    }
}
