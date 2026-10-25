package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.client.ui.FormField;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.net.LoginNames;

import java.util.function.Consumer;

/**
 * The Connect screen ({@code ui-spec.md} &sect;4.1): server, login and password, one Connect
 * button. A login the server doesn't know is offered as a new account, never made by itself
 * (review.md 2.3). There's no display name; other players see the
 * agency and director chosen per game on the Join Game screen.
 *
 * @author Mario Koehler
 */
public final class ConnectScreen extends StageScreen {

    private static final float COLUMN_WIDTH = 480f;
    private static final float LOGO_SIZE = 150f;

    private final FormField server;
    private final FormField login;
    private final FormField password;
    private final TextButton connect;
    private final Cell<Actor> bannerCell;

    /**
     * Builds the screen, prefilled with the server, login and password of the last successful login
     * (remembered between runs), so Enter logs straight in.
     *
     * @param game the game
     */
    public ConnectScreen(LaunchWindowGame game) {
        super(game);
        server = new FormField(ui, "Server", game.lastServer(), "host:port");
        login = new FormField(ui, "Login", game.lastLogin(), "your-login");
        password = new FormField(ui, "Password", game.lastPassword(), "");
        password.input().setPasswordMode(true);
        password.input().setPasswordCharacter('•');
        connect = ui.button("Connect", Theme.ButtonKind.PRIMARY);
        connect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submit();
            }
        });
        for (FormField field : new FormField[] {server, login, password}) {
            field.input().setTextFieldListener(this::submitOnEnter);
        }

        Table card = ui.card();
        card.defaults().growX().left().padBottom(Theme.SPACE_4);
        card.add(ui.label("Connect", Theme.TextStyle.EYEBROW, Theme.NAVY)).row();
        card.add(server).row();
        card.add(login).row();
        card.add(password).row();
        card.add(connect).row();
        card.add(ui.paragraph("New here? Connect with the login you want, then create the account. Logins use "
            + "letters, numbers, underscore or hyphen only, up to " + LoginNames.MAX_LENGTH + " characters.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
        card.add(ui.paragraph("There's no display name here. Other players see your agency name and director's name, "
            + "chosen per game after you connect.", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(0f);

        Table column = new Table();
        column.defaults().width(COLUMN_WIDTH);
        column.add(new Image(ui.logo(), Scaling.fit)).size(LOGO_SIZE).padBottom(Theme.SPACE_5).row();
        bannerCell = column.add((Actor) null);
        column.row();
        column.add(card);
        root.add(column);
    }

    @Override
    public void show() {
        super.show();
        TextField first = login.text().isEmpty() ? login.input() : password.input();
        stage.setKeyboardFocus(first);
    }

    /**
     * Returns the server field.
     *
     * @return the field
     */
    public FormField serverField() {
        return server;
    }

    /**
     * Returns the login field.
     *
     * @return the field
     */
    public FormField loginField() {
        return login;
    }

    /**
     * Returns the password field.
     *
     * @return the field
     */
    public FormField passwordField() {
        return password;
    }

    /**
     * Checks the form and, if it's complete, connects. Problems the client can see by itself
     * (an empty field, a login with characters the server would refuse, an address that isn't
     * one) are shown at once, without a round trip.
     */
    public void submit() {
        if (connect.isDisabled()) {
            return;
        }
        clearMessages();
        ServerAddress address = null;
        try {
            address = ServerAddress.parse(server.text());
        } catch (IllegalArgumentException e) {
            server.setError(e.getMessage());
        }
        if (login.text().isEmpty()) {
            login.setError("Required.");
        } else if (!LoginNames.isValid(login.text())) {
            login.setError("Use letters, numbers, _ or - only (max " + LoginNames.MAX_LENGTH + ").");
        }
        if (password.input().getText().isEmpty()) {
            password.setError("Required.");
        }
        if (address == null || !login.error().isEmpty() || !password.error().isEmpty()) {
            return;
        }
        connect(new Credentials(address, login.text(), password.input().getText()), false);
    }

    /**
     * Logs in, or creates the account first, and shows the outcome.
     *
     * @param credentials who and where
     * @param create      whether to create the account
     */
    private void connect(Credentials credentials, boolean create) {
        String serverText = server.text();
        setBusy(true);
        Consumer<LoginResult> done = result -> {
            if (result.kind() == LoginResult.Kind.IN_GAME || result.kind() == LoginResult.Kind.NEEDS_SLOT) {
                game.rememberConnect(serverText, credentials.login(), credentials.password());
            }
            if (isShowing()) {
                applyLoginResult(credentials, result);
            }
        };
        if (create) {
            game.gateway().createAccount(credentials, done);
        } else {
            game.gateway().logIn(credentials, done);
        }
    }

    /**
     * Shows a login's outcome: moves on to the agency or Join Game, or shows the refusal.
     *
     * @param credentials the credentials that were used
     * @param result      the outcome
     */
    public void applyLoginResult(Credentials credentials, LoginResult result) {
        setBusy(false);
        switch (result.kind()) {
            case IN_GAME -> {
                String problem = game.enterGame(credentials, result.state(), result.gameInfo());
                if (problem != null) {
                    showBanner(Theme.NoticeKind.DANGER, "Can't open this game.", problem);
                }
            }
            case NEEDS_SLOT -> game.setScreen(new JoinScreen(game, credentials));
            case VERSION_MISMATCH -> showBanner(Theme.NoticeKind.WARNING, "Versions don't match.",
                "Client v" + result.clientVersion() + ", server v" + result.serverVersion()
                    + ". Update the client and try again.");
            case REJECTED -> {
                password.setError(result.message());
                stage.setKeyboardFocus(password.input());
                password.input().selectAll();
            }
            case NO_ACCOUNT -> showNoAccount(credentials);
            case UNREACHABLE -> showBanner(Theme.NoticeKind.DANGER, "Can't reach the server.",
                "Check the address and try again.");
            case FAILED -> showBanner(Theme.NoticeKind.DANGER, "The connection broke off.",
                result.message().isEmpty() ? "Try again." : result.message());
        }
    }

    /**
     * Shows or ends the "connecting" state: the button is disabled and says so.
     *
     * @param busy whether a connect is under way
     */
    public void setBusy(boolean busy) {
        connect.setDisabled(busy);
        connect.setText(busy ? "Connecting…" : "Connect");
    }

    private void submitOnEnter(TextField field, char key) {
        if (key == '\r' || key == '\n') {
            submit();
        }
    }

    private void clearMessages() {
        server.setError(null);
        login.setError(null);
        password.setError(null);
        bannerCell.clearActor();
        bannerCell.padBottom(0f);
    }

    /**
     * A login the server doesn't know: a typo, or a new player. Offers to create the account with the
     * password as typed, never on its own (review.md 2.3).
     */
    private void showNoAccount(Credentials credentials) {
        Table offer = new Table();
        offer.add(ui.notice(Theme.NoticeKind.INFO, "No account named " + credentials.login() + " on this server.",
            "Check the spelling, or create it with the password you typed.")).growX().row();
        TextButton create = ui.button("Create account " + credentials.login(), Theme.ButtonKind.PRIMARY);
        create.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                clearMessages();
                connect(credentials, true);
            }
        });
        offer.add(create).right().padTop(Theme.SPACE_2);
        bannerCell.setActor(offer);
        bannerCell.padBottom(Theme.SPACE_4);
        root.invalidateHierarchy();
    }

    private void showBanner(Theme.NoticeKind kind, String lead, String text) {
        bannerCell.setActor(ui.notice(kind, lead, text));
        bannerCell.padBottom(Theme.SPACE_4);
        root.invalidateHierarchy();
    }
}
