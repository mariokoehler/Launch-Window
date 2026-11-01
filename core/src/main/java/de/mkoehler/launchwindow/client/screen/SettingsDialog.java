package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
import de.mkoehler.launchwindow.client.view.Credits;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;

import java.util.List;

/**
 * Settings ({@code ui-spec.md} &sect;4.12), a dialog over any session screen: the account and
 * Log out, the server and Switch server, sound, replays, and About. Opened by the header gear and
 * the hub strip's gear.
 *
 * @author Mario Koehler
 */
final class SettingsDialog {

    /** The dialog's width, from the mockup. */
    static final float WIDTH = 620f;

    /** The tallest the credits list grows before it scrolls. */
    private static final float CREDITS_HEIGHT = 520f;

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

        card.add(section("Text size")).padBottom(Theme.SPACE_2).row();
        Table textSize = new Table();
        textSize.add(textSizeControl()).width(220f).height(32f).padRight(Theme.SPACE_3);
        textSize.add(ui.paragraph("Large reads better on a small, sharp screen. For everyone on this device.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().fillX().left();
        card.add(textSize).padBottom(Theme.SPACE_4).row();

        card.add(section("Sound")).padBottom(Theme.SPACE_2).row();
        Table sound = new Table();
        sound.add(soundControl()).width(220f).height(32f).padRight(Theme.SPACE_3);
        sound.add(ui.paragraph("All the game's sound. For everyone on this device.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .expandX().fillX().left();
        card.add(sound).padBottom(Theme.SPACE_3).row();
        card.add(volume("Replay music", LaunchWindowGame.musicVolume(), LaunchWindowGame::setMusicVolume))
            .padBottom(Theme.SPACE_2).row();
        card.add(volume("Replay sounds", LaunchWindowGame.effectsVolume(), LaunchWindowGame::setEffectsVolume))
            .padBottom(Theme.SPACE_4).row();

        card.add(section("Replays")).padBottom(Theme.SPACE_2).row();
        Table replays = new Table();
        replays.add(replaysControl()).width(360f).height(32f).padRight(Theme.SPACE_3);
        replays.add(ui.paragraph("Watch a turn's missions before its report.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).expandX().fillX().left();
        card.add(replays).padBottom(Theme.SPACE_4).row();

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
        Table credits = new Table();
        credits.add(ui.paragraph("Fonts, photos and their licences, as each one asks to be credited.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().fillX().left().padRight(Theme.SPACE_3);
        credits.add(ChromeScreen.onClick(ui.button("Credits", Theme.ButtonKind.SECONDARY), this::openCredits))
            .right();
        card.add(credits).padBottom(Theme.SPACE_4).row();

        card.add(ChromeScreen.onClick(ui.button("Done", Theme.ButtonKind.PRIMARY), host::closeDialog)).right()
            .fill(false, false).row();
        return card;
    }

    /**
     * Opens the credits page ({@code assets/CREDITS.md}) in place of the settings, with a way back.
     */
    void openCredits() {
        Table card = host.dialogCard("Settings", "Credits");
        Table list = new Table();
        list.top().left();
        list.defaults().growX().left();
        list.add(ui.paragraph("Assets not made for Launch Window, with the credit their licence asks for.",
            Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        // The file's own intro talks about "this folder"; the paragraph above says it for the game.
        List<Credits.Section> sections = Credits.parse(ui.text("CREDITS.md")).stream()
            .filter(section -> !section.title().isEmpty()).toList();
        if (sections.isEmpty()) {
            list.add(ui.paragraph("The credits file is missing from this installation.", Theme.TextStyle.BODY_SM,
                Theme.INK_MUTED)).row();
        }
        for (Credits.Section section : sections) {
            if (!section.title().isEmpty()) {
                list.add(section(section.title())).padTop(Theme.SPACE_3).padBottom(Theme.SPACE_2).row();
            }
            for (Credits.Entry entry : section.entries()) {
                list.add(ui.paragraph((entry.bullet() ? "• " : "") + Credits.drawable(entry.text(),
                    Theme.CHARACTERS), Theme.TextStyle.BODY_SM, Theme.INK)).padBottom(Theme.SPACE_2).row();
            }
        }
        ScrollPane scroll = new ScrollPane(list, new ScrollPane.ScrollPaneStyle());
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        card.add(scroll).maxHeight(CREDITS_HEIGHT).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(ChromeScreen.onClick(ui.button("Back", Theme.ButtonKind.SECONDARY), this::open))
            .padRight(10f);
        buttons.add(ChromeScreen.onClick(ui.button("Done", Theme.ButtonKind.PRIMARY), host::closeDialog));
        card.add(buttons).right().fill(false, false).row();
        host.showDialog(card, WIDTH);
    }

    private Label section(String title) {
        return ui.label(title.toUpperCase(java.util.Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
    }

    /** The text size, for this device: standard or large (2026-10-26). */
    private Table textSizeControl() {
        Table segments = new Table();
        segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        segments.pad(Theme.BORDER_THIN);
        String[] names = {"Standard", "Large"};
        LaunchWindowGame.TextSize[] sizes = LaunchWindowGame.TextSize.values();
        for (int i = 0; i < sizes.length; i++) {
            LaunchWindowGame.TextSize size = sizes[i];
            boolean selected = LaunchWindowGame.textSize() == size;
            Table segment = new Table();
            segment.setBackground(ui.solid(selected ? Theme.NAVY : Theme.SURFACE_CARD));
            segment.add(ui.label(names[i], Theme.TextStyle.LABEL, selected ? Theme.ON_FILL : Theme.NAVY));
            segment.setTouchable(Touchable.enabled);
            segment.addListener(ChromeScreen.clicked(() -> {
                game.setTextSize(size);
                host.closeDialog();
                open();
            }));
            segments.add(segment).expand().fill().uniformX();
        }
        return segments;
    }

    /** Sound on or off, for this device (the splash reads it before anyone logs in). */
    private Table soundControl() {
        Table segments = new Table();
        segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        segments.pad(Theme.BORDER_THIN);
        for (boolean on : new boolean[] {true, false}) {
            boolean selected = LaunchWindowGame.soundOn() == on;
            Table segment = new Table();
            segment.setBackground(ui.solid(selected ? Theme.NAVY : Theme.SURFACE_CARD));
            segment.add(ui.label(on ? "On" : "Off", Theme.TextStyle.LABEL, selected ? Theme.ON_FILL : Theme.NAVY));
            segment.setTouchable(Touchable.enabled);
            segment.addListener(ChromeScreen.clicked(() -> {
                LaunchWindowGame.setSoundOn(on);
                host.closeDialog();
                open();
            }));
            segments.add(segment).expand().fill().uniformX();
        }
        return segments;
    }

    /**
     * A volume slider for the replay's sound, saved as it moves (2026-10-13).
     */
    private Table volume(String name, float value, java.util.function.Consumer<Float> save) {
        Table row = new Table();
        row.add(ui.label(name, Theme.TextStyle.BODY_SM, Theme.INK)).width(120f).left();
        com.badlogic.gdx.scenes.scene2d.ui.Slider slider =
            new com.badlogic.gdx.scenes.scene2d.ui.Slider(0f, 1f, 0.05f, false, ui.slider());
        slider.setValue(value);
        Label percent = ui.label(Math.round(value * 100) + " %", Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        slider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                save.accept(slider.getValue());
                percent.setText(Math.round(slider.getValue() * 100) + " %");
            }
        });
        row.add(slider).width(220f).padRight(Theme.SPACE_3);
        row.add(percent).expandX().left();
        return row;
    }

    /**
     * The choice about watching a turn's missions before its report ({@code ui-spec.md} &sect;4.4):
     * ask, always or never.
     */
    private Table replaysControl() {
        Table segments = new Table();
        segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        segments.pad(Theme.BORDER_THIN);
        String[] names = {"Ask", "Always", "Never"};
        LaunchWindowGame.ReplaysFirst[] choices = LaunchWindowGame.ReplaysFirst.values();
        for (int i = 0; i < choices.length; i++) {
            LaunchWindowGame.ReplaysFirst choice = choices[i];
            boolean selected = game.replaysFirst() == choice;
            Table segment = new Table();
            segment.setBackground(ui.solid(selected ? Theme.NAVY : Theme.SURFACE_CARD));
            segment.add(ui.label(names[i], Theme.TextStyle.LABEL, selected ? Theme.ON_FILL : Theme.NAVY));
            segment.setTouchable(Touchable.enabled);
            segment.addListener(ChromeScreen.clicked(() -> {
                game.setReplaysFirst(choice);
                host.closeDialog();
                open();
            }));
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
        game.rememberConnect(server, credentials.login(), credentials.password());
        game.gateway().logOut();
        host.closeDialog();
        game.setScreen(new ConnectScreen(game));
        game.setSession(null);
    }
}
