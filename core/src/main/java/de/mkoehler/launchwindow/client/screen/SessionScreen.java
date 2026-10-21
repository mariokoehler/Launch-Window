package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.ui.Countdown;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.Help;
import de.mkoehler.launchwindow.client.view.Tips;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * The base of every in-game screen, the hub included: the session, the action result toast, modal
 * dialogs, the submit-then-rebuild loop every action goes through, a refresh shortly after the
 * turn resolves, and the way to each built place.
 *
 * <p>Subclasses fill their content in {@link #rebuild()}, which runs on creation and, through
 * {@link #stateChanged()}, after every state change.</p>
 *
 * @author Mario Koehler
 */
abstract class SessionScreen extends StageScreen {

    private static final float TOAST_SECONDS = 3.4f;
    private static final float DIALOG_WIDTH = 520f;
    /** How long after the countdown reaches zero to wait before fetching the resolved turn. */
    private static final long RESOLUTION_GRACE_MILLIS = 5_000;
    /** The least time between automatic refreshes. */
    private static final long REFRESH_SPACING_MILLIS = 15_000;
    /** How long a server call may take before the waiting indicator shows, so quick calls don't flicker. */
    private static final float WAITING_DELAY_SECONDS = 0.3f;
    /** Where the indicator's middle sits, measured from the top: level with the header's chips. */
    private static final float WAITING_CENTRE_FROM_TOP = 46f;
    /** How long each of the indicator's dots stays lit. */
    private static final float WAITING_DOT_SECONDS = 0.25f;

    protected final ClientSession session;

    private Table dialog;
    private HelpOverlay help;
    private Table toast;
    private Table waiting;
    private final Image[] waitingDots = new Image[3];
    private float waitingSeconds;
    private boolean busy;
    private boolean refreshing;
    private long lastRefreshMillis = System.currentTimeMillis();

    /**
     * Takes the session from the game.
     *
     * @param game the game, in a session
     */
    protected SessionScreen(LaunchWindowGame game) {
        super(game);
        this.session = game.session();
        // F1 opens help from anywhere in the game, Esc closes it (ui-spec.md component 23).
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.F1 && help == null) {
                    openHelp();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE && help != null) {
                    closeHelp();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Fills {@link #content} from the session's current state. Runs once the subclass is built
     * (call it at the end of the subclass constructor) and after every state change.
     */
    protected abstract void rebuild();

    /**
     * Where the back button goes. The hub by default.
     */
    protected void goBack() {
        goHome();
    }

    /**
     * Goes to the Space Complex hub.
     */
    protected void goHome() {
        game.setScreen(new SpaceComplexScreen(game));
    }

    /**
     * Goes to the SET Centre's personnel overview.
     */
    protected void goSetCentre() {
        game.setScreen(new SetCentreScreen(game));
    }

    /**
     * Goes to Headquarters' landing screen.
     */
    protected void goHeadquarters() {
        game.setScreen(new HeadquartersScreen(game));
    }

    /**
     * Goes to Mission Control's flight-controller overview.
     */
    protected void goMissionControl() {
        game.setScreen(new MissionControlScreen(game));
    }

    /**
     * Goes to the VAB's landing screen.
     */
    protected void goVab() {
        game.setScreen(new VabScreen(game));
    }

    /**
     * Goes to the Astronaut Complex.
     */
    protected void goAstronautComplex() {
        game.setScreen(new AstronautComplexScreen(game));
    }

    /**
     * Goes to the Public Affairs Office.
     */
    protected void goPublicAffairs() {
        game.setScreen(new PublicAffairsScreen(game));
    }

    /**
     * Goes to the Intelligence Office's overview.
     */
    protected void goIntelligenceOffice() {
        game.setScreen(new IntelligenceOfficeScreen(game, IntelligenceOfficeScreen.Tab.OVERVIEW));
    }

    /**
     * Goes to the Museum, on the race.
     */
    protected void goMuseum() {
        game.setScreen(new MuseumScreen(game, MuseumScreen.Tab.RACE));
    }

    /**
     * Fetches the state and game info again and rebuilds the screen with them. Quiet: a failed
     * refresh changes nothing and says nothing; the next action reports any real problem.
     */
    protected void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        lastRefreshMillis = System.currentTimeMillis();
        long resolvedBefore = session.latestResolvedTurn();
        game.gateway().refresh(session.credentials(), result -> {
            refreshing = false;
            if (isShowing() && result.kind() == ServerResult.Kind.OK && result.state() != null) {
                session.update(result);
                stateChanged();
                if (session.latestResolvedTurn() > resolvedBefore) {
                    // A turn resolved while the player was here: a new front page comes first
                    // (ui-spec.md section 4.18), then say so rather than take over the screen.
                    withFrontPages(() -> toast("The turn resolved. Its report is waiting on the Space Complex.",
                        Theme.NoticeKind.INFO));
                }
            }
        });
    }

    /**
     * Submits an action: while it's under way further submits are ignored; on success the new
     * state is taken in and the screen rebuilt, and on a refusal or failure the toast says why.
     *
     * @param action    the action
     * @param onSuccess runs after a successful submit's rebuild, with the result (may be {@code null})
     */
    protected void submit(PlayerAction action, Consumer<ServerResult> onSuccess) {
        if (busy) {
            return;
        }
        busy = true;
        game.gateway().submit(session.credentials(), action, session.openTurn(), result -> {
            busy = false;
            if (!isShowing()) {
                return;
            }
            handle(result, onSuccess);
        });
    }

    /**
     * Takes in a server result: updates the session and rebuilds on success, or toasts the problem.
     *
     * @param result    the result
     * @param onSuccess runs after a successful rebuild, may be {@code null}
     */
    protected void handle(ServerResult result, Consumer<ServerResult> onSuccess) {
        switch (result.kind()) {
            case OK -> {
                session.update(result);
                stateChanged();
                if (onSuccess != null) {
                    onSuccess.accept(result);
                }
                if (result.state() == null) {
                    // Applied, but the state didn't come back with it.
                    refresh();
                }
            }
            case REFUSED -> {
                toast(Refusals.text(result.message()), Theme.NoticeKind.INFO);
                // A refusal often means the screen is out of date (a resolved turn refreshed the pool, say).
                refresh();
            }
            case UNREACHABLE -> toast("Can't reach the server. Try again.", Theme.NoticeKind.DANGER);
            case LOGIN_FAILED -> toast("Couldn't log in again: " + result.message(), Theme.NoticeKind.DANGER);
            case FAILED -> toast("The connection broke off. Try again.", Theme.NoticeKind.DANGER);
            case TURN_RESOLVED -> turnResolvedUnderfoot(result);
        }
    }

    /**
     * The turn resolved before an action arrived, so the server applied nothing: take in the new
     * turn's state and say so, and the player decides again with it in front of them.
     *
     * @param result the result, with the new state when it could be fetched
     */
    protected void turnResolvedUnderfoot(ServerResult result) {
        if (result.state() != null) {
            session.update(result);
            stateChanged();
        } else {
            refresh();
        }
        toast("The turn resolved before that reached the server, so nothing was done. Its report is waiting "
            + "on the Space Complex.", Theme.NoticeKind.INFO);
    }

    /**
     * Attaches a hover tooltip and returns the actor, for use inside {@code add(...)}.
     *
     * @param actor the actor
     * @param tip   what it means
     * @param <A>   the actor's type
     * @return the actor
     */
    protected <A extends Actor> A tip(A actor, Tips.Tip tip) {
        ui.tooltip(actor, tip.title(), tip.text());
        return actor;
    }

    /**
     * Returns whether a submit is under way.
     *
     * @return whether busy
     */
    protected boolean isBusy() {
        return busy;
    }

    /**
     * Shows the action result toast in the bottom-right corner for a few seconds
     * ({@code ui-spec.md} component 6): green for success, navy for a refusal.
     *
     * @param text the message
     * @param kind the notice kind
     */
    public void toast(String text, Theme.NoticeKind kind) {
        if (toast != null) {
            toast.remove();
        }
        toast = new Table();
        toast.add(ui.notice(kind, text)).width(420f);
        toast.pack();
        toast.setPosition(stage.getWidth() - toast.getWidth() - 28f, 28f);
        toast.setTouchable(Touchable.disabled);
        Table shown = toast;
        toast.addAction(Actions.sequence(Actions.delay(TOAST_SECONDS), Actions.fadeOut(0.2f), Actions.run(() -> {
            shown.remove();
            if (toast == shown) {
                toast = null;
            }
        })));
        stage.addActor(toast);
    }

    /**
     * Shows a modal dialog: a navy scrim over the screen with the card centered on it
     * ({@code ui-spec.md} components 5, 12 and 13). Only one at a time.
     *
     * @param card the dialog's content, already built
     */
    public void showDialog(Table card) {
        showDialog(card, DIALOG_WIDTH);
    }

    /**
     * Shows a modal dialog of a given width, for the wider dialogs (Settings, the game result).
     *
     * @param card  the dialog's content, already built
     * @param width the card's width
     */
    public void showDialog(Table card, float width) {
        closeDialog();
        dialog = new Table();
        dialog.setFillParent(true);
        dialog.setBackground(ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.55f)));
        dialog.setTouchable(Touchable.enabled);
        dialog.addListener(new ClickListener());
        dialog.add(card).width(width);
        stage.addActor(dialog);
        if (toast != null) {
            toast.toFront();
        }
    }

    /**
     * Opens Settings ({@code ui-spec.md} &sect;4.12) over this screen.
     */
    public void openSettings() {
        new SettingsDialog(this).open();
    }

    @Override
    public void show() {
        super.show();
        if (help != null) {
            // Back from an explainer visited from help: its chip's unread dot is gone.
            help.refresh();
            help.focus();
        }
    }

    /**
     * Returns where help opened from this screen starts: the screen's own topics. {@code null}, the
     * default, opens help on every topic.
     *
     * @return the context, or {@code null}
     */
    protected Help.Context helpContext() {
        return null;
    }

    /**
     * Opens help ({@code ui-spec.md} component 23) over this screen, filtered to its topics. Help
     * sits above any dialog, which stays as it was underneath.
     *
     * @return the help, or the one already open
     */
    public HelpOverlay openHelp() {
        return openHelp(helpContext());
    }

    /**
     * Opens help over this screen, filtered to some topics: a screen's context, or an advisor
     * item's.
     *
     * @param context the topics, or {@code null} for every topic
     * @return the help, or the one already open
     */
    public HelpOverlay openHelp(Help.Scope context) {
        if (help == null) {
            game.markHelpOpened();
            help = new HelpOverlay(this, context);
            stage.addActor(help.actor());
            help.focus();
            if (toast != null) {
                toast.toFront();
            }
        }
        return help;
    }

    /**
     * Closes help, if it's open.
     */
    public void closeHelp() {
        if (help != null) {
            help.actor().remove();
            help = null;
            stage.setKeyboardFocus(null);
        }
    }

    /**
     * Opens the credits page from Settings' About section over this screen.
     */
    public void openCredits() {
        new SettingsDialog(this).openCredits();
    }

    /**
     * Closes the dialog, if one is open.
     */
    public void closeDialog() {
        if (dialog != null) {
            dialog.remove();
            dialog = null;
        }
    }

    /**
     * Builds a dialog card: white, a navy outline, an eyebrow and a title, ready for more rows.
     *
     * @param eyebrow the eyebrow
     * @param title   the title
     * @return the card
     */
    protected Table dialogCard(String eyebrow, String title) {
        return dialogCard(eyebrow, title, null);
    }

    /**
     * Builds a dialog card with a history note's icon after its title (component 15). The title is one line:
     * keep it short.
     *
     * @param eyebrow     the eyebrow
     * @param title       the title
     * @param historyNote the note's text, or null for none
     * @return the card
     */
    protected Table dialogCard(String eyebrow, String title, String historyNote) {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(Theme.SPACE_5 + Theme.BORDER_EMPHASIS);
        card.defaults().growX().left();
        card.add(ui.label(eyebrow, Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        if (historyNote == null) {
            card.add(ui.paragraph(title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_4).row();
        } else {
            Table head = new Table();
            head.add(ui.label(title, Theme.TextStyle.HEADING_SM, Theme.INK));
            head.add(new HistoryNote(ui, historyNote)).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2);
            head.add().expandX();
            card.add(head).padBottom(Theme.SPACE_4).row();
        }
        return card;
    }

    /**
     * Says that a place in the game isn't built yet: footer chips and buttons for screens that
     * don't exist in the client so far show this instead of doing nothing.
     *
     * @param place the place, such as "Mission Control"
     */
    protected void notBuiltYet(String place) {
        toast(place + " isn't built yet.", Theme.NoticeKind.INFO);
    }

    /**
     * Returns a listener that runs an action on click.
     *
     * @param action the action
     * @return the listener
     */
    protected static ClickListener clicked(Runnable action) {
        return new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        };
    }

    /**
     * Opens the latest turn's report, or one mission's replay from it, fetching the report if this
     * session hasn't yet. Says so when there's no report (no turn resolved since the player joined)
     * or the mission didn't fly in it.
     *
     * @param missionId the mission to replay, or {@code null} for the report
     */
    protected void openLatestReport(String missionId) {
        openLatestReport(missionId, false);
    }

    /**
     * Opens the latest turn's report on entering the game: like {@link #openLatestReport(String)},
     * but quiet when there's nothing to open, since the player didn't ask.
     */
    protected void openUnreadReport() {
        openLatestReport(null, true);
    }

    /**
     * Gets the latest turn's report, from the session or the server, without opening it.
     *
     * @param found     what to do with it, if this screen still shows
     * @param otherwise what to do when there's none or the fetch failed
     */
    protected void fetchLatestReport(java.util.function.Consumer<TurnReport> found, Runnable otherwise) {
        long turn = session.latestResolvedTurn();
        TurnReport cached = session.report(turn);
        if (cached != null) {
            found.accept(cached);
            return;
        }
        game.gateway().fetchTurnReport(session.credentials(), -1, result -> {
            if (!isShowing()) {
                return;
            }
            switch (result.kind()) {
                case FOUND -> {
                    session.keepReport(result.report());
                    found.accept(result.report());
                }
                case NONE -> {
                    game.markReportRead(turn);
                    session.noReportAt(turn);
                    otherwise.run();
                }
                case FAILED -> otherwise.run();
            }
        });
    }

    private void openLatestReport(String missionId, boolean quiet) {
        long turn = session.latestResolvedTurn();
        TurnReport cached = session.report(turn);
        if (cached != null) {
            openReport(cached, missionId);
            return;
        }
        game.gateway().fetchTurnReport(session.credentials(), -1, result -> {
            if (!isShowing()) {
                return;
            }
            switch (result.kind()) {
                case FOUND -> {
                    session.keepReport(result.report());
                    openReport(result.report(), missionId);
                }
                case NONE -> {
                    game.markReportRead(turn);
                    session.noReportAt(turn);
                    if (!quiet) {
                        toast("No turn report yet: none has resolved since you joined.", Theme.NoticeKind.INFO);
                    }
                }
                case FAILED -> {
                    if (!quiet) {
                        toast("Couldn't fetch the turn report. Try again.", Theme.NoticeKind.DANGER);
                    }
                }
            }
        });
    }

    /**
     * Opens an earlier (or later) turn's report, fetching it if this session hasn't yet. Says so
     * when the turn resolved before the player joined, and rebuilds so the screen stops offering it.
     *
     * @param turn the turn
     */
    protected void openReportAt(long turn) {
        TurnReport cached = session.report(turn);
        if (cached != null) {
            game.setScreen(new TurnReportScreen(game, cached));
            return;
        }
        game.gateway().fetchTurnReport(session.credentials(), turn, result -> {
            if (!isShowing()) {
                return;
            }
            switch (result.kind()) {
                case FOUND -> {
                    session.keepReport(result.report());
                    game.setScreen(new TurnReportScreen(game, result.report()));
                }
                case NONE -> {
                    session.noReportAt(turn);
                    rebuild();
                    toast("No report for that turn: it resolved before you joined.", Theme.NoticeKind.INFO);
                }
                case FAILED -> toast("Couldn't fetch the turn report. Try again.", Theme.NoticeKind.DANGER);
            }
        });
    }

    /**
     * Opens a report, marking it read, or one of its missions' replays.
     *
     * @param report    the report
     * @param missionId the mission to replay, or {@code null} for the report
     */
    protected void openReport(TurnReport report, String missionId) {
        game.markReportRead(report.gameTurn());
        if (missionId == null) {
            game.setScreen(new TurnReportScreen(game, report));
        } else if (report.missions().stream().anyMatch(flown -> flown.missionInstanceId().equals(missionId))) {
            game.setScreen(new ReplayScreen(game, report, missionId));
        } else {
            toast("That mission didn't fly in the last turn.", Theme.NoticeKind.INFO);
        }
    }

    /**
     * Fetches the front pages and shows the unseen ones over this screen, coming back to it
     * afterwards ({@code ui-spec.md} &sect;4.18); then runs {@code after} here. With none unseen,
     * or if the fetch fails, it just runs {@code after}.
     *
     * @param after what to do once the pages are out of the way
     */
    protected void withFrontPages(Runnable after) {
        fetchFrontPages(() -> {
            List<FrontPage> unseen = FrontPageScreen.unseen(game);
            if (unseen.isEmpty()) {
                after.run();
                return;
            }
            SessionScreen self = this;
            game.visit(new FrontPageScreen(game, unseen, () -> {
                game.setScreen(self);
                after.run();
            }, false));
        });
    }

    /**
     * Fetches every front page into the session, then runs {@code after} if this screen still
     * shows, whether the fetch worked or not.
     *
     * @param after what to do next
     */
    protected void fetchFrontPages(Runnable after) {
        game.gateway().fetchFrontPages(session.credentials(), result -> {
            if (result.ok()) {
                session.keepFrontPages(result.pages());
            }
            if (isShowing()) {
                after.run();
            }
        });
    }

    /**
     * Reopens the page that tells a First over this screen, coming back to it afterwards
     * ({@code ui-spec.md} &sect;4.18): from the Museum's race and the turn report's goal line.
     *
     * @param goalId the First's goal
     */
    protected void openFrontPage(String goalId) {
        Runnable open = () -> FrontPageScreen.pageFor(game, goalId).ifPresentOrElse(page -> {
            SessionScreen self = this;
            game.visit(new FrontPageScreen(game, List.of(page), () -> game.setScreen(self), true));
        }, () -> toast("That front page isn't here.", Theme.NoticeKind.INFO));
        if (session.frontPages() == null || FrontPageScreen.pageFor(game, goalId).isEmpty()) {
            fetchFrontPages(open);
        } else {
            open.run();
        }
    }

    /**
     * Runs after the session took in a new state: rebuilds the screen. Screens with more than
     * their content to update (the chrome's chips) override it.
     */
    protected void stateChanged() {
        rebuild();
    }

    /**
     * Returns the next-turn countdown's text, upper case, as the chips show it.
     *
     * @return the text
     */
    protected String nextTurnText() {
        return Countdown.nextTurn(session.gameInfo().millisUntilNextResolution(System.currentTimeMillis()))
            .toUpperCase(Locale.ROOT);
    }

    @Override
    public void render(float delta) {
        long now = System.currentTimeMillis();
        OptionalLong left = session.gameInfo().millisUntilNextResolution(now);
        // The server says a turn resolved: fetch it now rather than at the next poll.
        if (!refreshing && game.gateway().takeTurnResolvedNotice() > 0) {
            refresh();
        }
        // Once the turn is due, fetch the resolved state shortly after: new date, new candidates, new countdown.
        if (left.isPresent() && left.getAsLong() == 0 && now - lastRefreshMillis > REFRESH_SPACING_MILLIS
            && now - session.gameInfo().receivedAtMillis() > RESOLUTION_GRACE_MILLIS) {
            refresh();
        }
        updateWaiting(delta);
        super.render(delta);
    }

    /**
     * Shows a small "waiting for the server" chip at the top centre while a server call
     * takes longer than {@link #WAITING_DELAY_SECONDS}, so a slow answer doesn't look like a hang.
     * Three dots light up in turn while it waits.
     *
     * @param delta the seconds since the last frame
     */
    private void updateWaiting(float delta) {
        if (!game.gateway().isWaiting()) {
            waitingSeconds = 0f;
            if (waiting != null) {
                waiting.remove();
                waiting = null;
            }
            return;
        }
        waitingSeconds += delta;
        if (waitingSeconds < WAITING_DELAY_SECONDS) {
            return;
        }
        if (waiting == null) {
            waiting = new Table();
            waiting.setBackground(ui.solid(Theme.NAVY));
            waiting.pad(8f, 14f, 8f, 14f);
            Table dots = new Table();
            for (int i = 0; i < waitingDots.length; i++) {
                waitingDots[i] = new Image(ui.solid(Theme.ON_FILL));
                dots.add(waitingDots[i]).size(6f).padRight(i < waitingDots.length - 1 ? 4f : 0f);
            }
            waiting.add(dots).padRight(10f);
            waiting.add(ui.label("WAITING FOR THE SERVER", Theme.TextStyle.LABEL, Theme.ON_FILL));
            waiting.pack();
            // Centred in the header row, which is empty in the middle on every screen, the hub included.
            waiting.setPosition((stage.getWidth() - waiting.getWidth()) / 2f,
                stage.getHeight() - WAITING_CENTRE_FROM_TOP - waiting.getHeight() / 2f);
            waiting.setTouchable(Touchable.disabled);
            stage.addActor(waiting);
        }
        waiting.toFront();
        int lit = (int) ((waitingSeconds - WAITING_DELAY_SECONDS) / WAITING_DOT_SECONDS) % waitingDots.length;
        for (int i = 0; i < waitingDots.length; i++) {
            waitingDots[i].getColor().a = i == lit ? 1f : 0.35f;
        }
    }
}
