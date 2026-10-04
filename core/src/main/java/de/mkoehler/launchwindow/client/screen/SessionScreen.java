package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.ui.Countdown;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.TurnReport;

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

    protected final ClientSession session;

    private Table dialog;
    private Table toast;
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
                    // A turn resolved while the player was here: say so rather than take over the screen.
                    toast("The turn resolved. Its report is waiting on the Space Complex.", Theme.NoticeKind.INFO);
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
        game.gateway().submit(session.credentials(), action, result -> {
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
        }
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
        closeDialog();
        dialog = new Table();
        dialog.setFillParent(true);
        dialog.setBackground(ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.55f)));
        dialog.setTouchable(Touchable.enabled);
        dialog.addListener(new ClickListener());
        dialog.add(card).width(DIALOG_WIDTH);
        stage.addActor(dialog);
        if (toast != null) {
            toast.toFront();
        }
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
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(Theme.SPACE_5 + Theme.BORDER_EMPHASIS);
        card.defaults().growX().left();
        card.add(ui.label(eyebrow, Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        card.add(ui.paragraph(title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_4).row();
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

    private void openReport(TurnReport report, String missionId) {
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
        // Once the turn is due, fetch the resolved state shortly after: new date, new candidates, new countdown.
        if (left.isPresent() && left.getAsLong() == 0 && now - lastRefreshMillis > REFRESH_SPACING_MILLIS
            && now - session.gameInfo().receivedAtMillis() > RESOLUTION_GRACE_MILLIS) {
            refresh();
        }
        super.render(delta);
    }
}
