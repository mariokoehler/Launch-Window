package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.client.ui.FormField;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;

/**
 * Settings ({@code ui-spec.md} &sect;4.12), a dialog over any session screen: the account and
 * Log out, the server and Switch server, sound (later), and About. Opened by the header gear and
 * the hub strip's gear.
 *
 * @author Mario Koehler
 */
final class SettingsDialog {

    /** The dialog's width, from the mockup. */
    static final float WIDTH = 620f;

    private final SessionScreen host;
    private final LaunchWindowGame game;
    private final UiKit ui;

    /**
     * Prepares the dialog for a screen.
     *
     * @param host the screen it opens over
     */
    SettingsDialog(SessionScreen host) {
        this.host = host;
        this.game = host.game;
        this.ui = host.ui;
    }

    /**
     * Opens the dialog.
     */
    void open() {
        host.showDialog(build(), WIDTH);
    }

    private Table build() {
        ClientSession session = host.session;
        Credentials credentials = session.credentials();
        Table card = host.dialogCard("Launch Window", "Settings");

        card.add(section("Account")).padBottom(Theme.SPACE_2).row();
        Table account = new Table();
        Table who = new Table();
        who.defaults().left();
        who.add(ui.label("Logged in as " + credentials.login(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        who.add(ui.label("on " + credentials.address() + " · connected", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        account.add(who).expandX().left();
        account.add(ChromeScreen.onClick(ui.button("Log out", Theme.ButtonKind.SECONDARY), this::logOut)).right();
        card.add(account).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("Your actions for today are already saved on the server. A mission plan you haven't launched "
            + "is dropped.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();

        card.add(section("Server")).padBottom(Theme.SPACE_2).row();
        Table server = new Table();
        String current = credentials.address().toString();
        FormField address = new FormField(ui, "Address", current, "host:port");
        TextButton switchServer = ChromeScreen.onClick(ui.button("Switch server", Theme.ButtonKind.SECONDARY),
            () -> switchServer(address));
        switchServer.setDisabled(true);
        address.input().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchServer.setDisabled(address.text().trim().isEmpty() || address.text().trim().equals(current));
            }
        });
        server.add(address).expandX().fillX().bottom().padRight(Theme.SPACE_3);
        server.add(switchServer).bottom();
        card.add(server).padBottom(Theme.SPACE_2).row();
        card.add(ui.paragraph("Switching logs you out and goes back to Connect, with the new address filled in.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();

        card.add(section("Sound")).padBottom(Theme.SPACE_2).row();
        Table sound = new Table();
        sound.add(soundControl()).width(220f).height(32f).padRight(Theme.SPACE_3);
        sound.add(ui.label("Coming later: the game has no sound yet.", Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .expandX().left();
        card.add(sound).padBottom(Theme.SPACE_4).row();

        GameInfoResponse info = session.gameInfo().response();
        card.add(section("About")).padBottom(Theme.SPACE_2).row();
        card.add(ui.label("Launch Window " + AppVersion.getVersion(), Theme.TextStyle.BODY_STRONG, Theme.INK))
            .padBottom(2f).row();
        card.add(ui.paragraph("This game: content pack " + info.contentPackId() + " (" + shortHash(
            info.contentPackHash()) + "), the date " + info.date().label() + ", turn " + info.gameTurn() + ".",
            Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(2f).row();
        card.add(ui.paragraph("A non-commercial fan project inspired by Buzz Aldrin's Space Program Manager. Not "
            + "affiliated with or endorsed by its makers.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED))
            .padBottom(2f).row();
        card.add(ui.paragraph("Fonts: Archivo and Work Sans, under the SIL Open Font License.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();

        card.add(ChromeScreen.onClick(ui.button("Done", Theme.ButtonKind.PRIMARY), host::closeDialog)).right()
            .fill(false, false).row();
        return card;
    }

    private Label section(String title) {
        return ui.label(title.toUpperCase(java.util.Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
    }

    /** Sound's On/Off control, both segments disabled until the game has sound. */
    private Table soundControl() {
        Table segments = new Table();
        segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        segments.pad(Theme.BORDER_THIN);
        for (String option : new String[] {"On", "Off"}) {
            boolean selected = option.equals("Off");
            Table segment = new Table();
            // Disabled: the selected segment is only a light navy tint, not the solid navy of a live one.
            segment.setBackground(ui.solid(selected ? new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.15f)
                : Theme.SURFACE_CARD));
            segment.add(ui.label(option, Theme.TextStyle.LABEL, Theme.INK_MUTED));
            segments.add(segment).expand().fill().uniformX();
        }
        return segments;
    }

    private static String shortHash(String hash) {
        return hash == null ? "unknown" : hash.substring(0, Math.min(8, hash.length()));
    }

    private void logOut() {
        leave(host.session.credentials().address().toString());
    }

    private void switchServer(FormField address) {
        String text = address.text().trim();
        try {
            ServerAddress.parse(text);
        } catch (IllegalArgumentException e) {
            address.setError(e.getMessage());
            return;
        }
        leave(text);
    }

    /** Ends the session and goes back to Connect, with the given server filled in. */
    private void leave(String server) {
        Credentials credentials = host.session.credentials();
        game.rememberConnect(server, credentials.login());
        game.gateway().logOut();
        host.closeDialog();
        game.setScreen(new ConnectScreen(game));
        game.setSession(null);
    }
}
