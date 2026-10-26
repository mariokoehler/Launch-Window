package de.mkoehler.launchwindow.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.NetworkGateway;
import de.mkoehler.launchwindow.client.connect.ServerGateway;
import de.mkoehler.launchwindow.client.screen.ConnectScreen;
import de.mkoehler.launchwindow.client.screen.SpaceComplexScreen;
import de.mkoehler.launchwindow.client.settings.ClientSettings;
import de.mkoehler.launchwindow.client.settings.SettingsStore;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * The client application's shared entry point ({@code design.md} &sect;3.3: lwjgl3 stays a
 * thin launcher, core holds the actual client). Owns the widget kit and the server gateway for
 * the whole run and starts on the Connect screen.
 *
 * @author Mario Koehler
 */
public class LaunchWindowGame extends Game {

    private static final float MIN_FONT_SCALE = 0.5f;
    private static final float MAX_FONT_SCALE = 3f;
    /** How long the window's size has to stay put before the fonts are rebuilt for it. */
    private static final long FONT_REBUILD_DELAY_MS = 250;

    /** The sound setting when kept in memory (screenshots), else {@code null}. */
    private static Boolean soundInMemory;
    /** The volumes when the sound setting is kept in memory. */
    private static final java.util.Map<String, Float> volumesInMemory = new java.util.HashMap<>();
    /** The replay's music and effects volume until the player sets them (unconfirmed). */
    private static final float DEFAULT_VOLUME = 0.7f;
    /** The text size when kept in memory (screenshots), else {@code null}. */
    private static TextSize textSizeInMemory;
    private UiKit ui;
    private ServerGateway gateway;
    private SettingsStore settingsStore;
    private ClientSettings settings = ClientSettings.defaults();
    private ClientSession session;
    private final Hub.Clock hubClock = new Hub.Clock();
    /** Whether the hub's advisor card is minimized to its head; for this client run only. */
    private boolean advisorMinimized;
    /** The explainers' read marks when kept in memory (screenshots), else {@code null}. */
    private java.util.Set<String> explainersReadInMemory;
    private java.util.Set<String> archiveReadInMemory;
    private java.util.Set<String> frontPagesSeenInMemory;
    /** The front pages' filler records when kept in memory (screenshots), else {@code null}. */
    private java.util.Map<String, String> fillersInMemory;
    private java.util.Set<String> flagsInMemory;
    private float pendingFontScale;
    private long lastResizeMillis;

    @Override
    public void create() {
        float[] scale = Theme.Fonts.windowScale(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        ui = new UiKit(Gdx.files.internal(""), fontScale(scale[1]));
        ui.fonts().setPixelGrid(scale[0], scale[1]);
        ui.fonts().setTextScale(textSize().scale());
        pendingFontScale = ui.fonts().scale();
        gateway = createGateway();
        settingsStore = createSettingsStore();
        settings = settingsStore.load();
        setScreen(new ConnectScreen(this));
    }

    /**
     * Creates the store the Connect screen's server, login and password are remembered in. The
     * offscreen screenshot tool overrides this, so it never reads the player's real file.
     *
     * @return the store
     */
    protected SettingsStore createSettingsStore() {
        return SettingsStore.inHomeDirectory();
    }

    /**
     * Creates a video player for the replay's clips ({@code ui-spec.md} &sect;4.7.3), or returns
     * {@code null} when none can play here (no decoder, no natives): the replay then shows stills.
     * The offscreen screenshot tool returns {@code null}, so its shots don't depend on timing.
     *
     * @return the player, or {@code null}
     */
    public com.badlogic.gdx.video.VideoPlayer createVideoPlayer() {
        try {
            return com.badlogic.gdx.video.VideoPlayerCreator.createVideoPlayer();
        } catch (RuntimeException | LinkageError e) {
            Gdx.app.error("Video", "no video player", e);
            return null;
        }
    }

    /**
     * Creates the server gateway. The offscreen screenshot tool overrides this with a stub.
     *
     * @return the gateway
     */
    protected ServerGateway createGateway() {
        return new NetworkGateway();
    }

    /**
     * Returns the widget kit.
     *
     * @return the kit
     */
    public UiKit ui() {
        return ui;
    }

    /**
     * Returns the server gateway.
     *
     * @return the gateway
     */
    public ServerGateway gateway() {
        return gateway;
    }

    /**
     * Enters the game after a successful login or claim: starts the session (loading the game's
     * content pack) and opens the Space Complex hub.
     *
     * @param credentials who and where
     * @param state       the player's state
     * @param gameInfo    the game info
     * @return {@code null} on success, or why the game couldn't be entered (the client lacks the
     *     game's content pack)
     */
    public String enterGame(Credentials credentials, PlayerSnapshot state, GameInfo gameInfo) {
        try {
            session = ClientSession.start(credentials, state, gameInfo, LaunchWindowGame.class.getClassLoader());
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
        setScreen(new SpaceComplexScreen(this, true));
        return null;
    }

    /**
     * Returns the latest turn whose report this player has read on this device. Kept in the
     * client's preferences per server and login, so it's per device (unconfirmed).
     *
     * @return the turn, or -1 if none was read
     */
    public long lastReadReport() {
        if (session == null) {
            return -1;
        }
        return preferences().getLong(readKey(), -1);
    }

    /**
     * Marks a turn's report as read, if it's newer than the one read before.
     *
     * @param turn the turn
     */
    public void markReportRead(long turn) {
        if (session == null || turn <= lastReadReport()) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putLong(readKey(), turn);
        preferences.flush();
    }

    /**
     * Returns whether a resolved turn's report hasn't been read yet.
     *
     * @return whether one is unread
     */
    public boolean reportUnread() {
        return session != null && session.latestResolvedTurn() >= 1 && session.latestResolvedTurn() > lastReadReport();
    }

    /**
     * Returns whether the game has a result this player hasn't seen announced on this device yet,
     * for the game result dialog ({@code ui-spec.md} &sect;4.13). Kept like the report's read mark.
     *
     * @return whether to announce the result
     */
    public boolean resultUnseen() {
        return session != null && session.gameInfo().response().decided()
            && !preferences().getBoolean(key("resultSeen"), false);
    }

    /**
     * Marks the game result as seen, once the player closed its announcement.
     */
    public void markResultSeen() {
        if (session == null) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean(key("resultSeen"), true);
        preferences.flush();
    }

    /**
     * Returns whether this player has opened an explainer on this device, for the concept chips'
     * and the Museum's "new" dot ({@code ui-spec.md} component 19). Kept in the client's
     * preferences per server and login, like the report's read mark.
     *
     * @param explainerId the explainer
     * @return whether it was read
     */
    public boolean explainerRead(String explainerId) {
        if (explainersReadInMemory != null) {
            return explainersReadInMemory.contains(explainerId);
        }
        return session != null && preferences().getBoolean(key("explainerRead." + explainerId), false);
    }

    /**
     * Marks an explainer as read, as soon as any of its sheets opens.
     *
     * @param explainerId the explainer
     */
    public void markExplainerRead(String explainerId) {
        if (explainersReadInMemory != null) {
            explainersReadInMemory.add(explainerId);
            return;
        }
        if (session == null || explainerRead(explainerId)) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean(key("explainerRead." + explainerId), true);
        preferences.flush();
    }

    /**
     * Keeps the explainers' read marks in memory instead of the preferences, starting from the
     * given set, so that screenshots don't depend on what this machine has read before.
     *
     * @param read the explainers to treat as read; the set is changed as more are read
     */
    public void useExplainerReadMarksInMemory(java.util.Set<String> read) {
        explainersReadInMemory = read;
    }

    /**
     * Returns whether this player has opened an archive page on this device, for the Museum's "new"
     * dot ({@code ui-spec.md} &sect;4.19). Kept like the explainers' read marks.
     *
     * @param pageId the page
     * @return whether it was opened
     */
    public boolean archiveRead(String pageId) {
        if (archiveReadInMemory != null) {
            return archiveReadInMemory.contains(pageId);
        }
        return session != null && preferences().getBoolean(key("archiveRead." + pageId), false);
    }

    /**
     * Marks an archive page as opened.
     *
     * @param pageId the page
     */
    public void markArchiveRead(String pageId) {
        if (archiveReadInMemory != null) {
            archiveReadInMemory.add(pageId);
            return;
        }
        if (session == null || archiveRead(pageId)) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean(key("archiveRead." + pageId), true);
        preferences.flush();
    }

    /**
     * Keeps the archive's read marks in memory instead of the preferences (screenshots).
     *
     * @param read the pages to treat as opened; the set is changed as more are opened
     */
    public void useArchiveReadMarksInMemory(java.util.Set<String> read) {
        archiveReadInMemory = read;
    }

    /**
     * Returns whether this player has seen a front page on this device ({@code ui-spec.md}
     * &sect;4.18). Kept like the report's read mark, per page.
     *
     * @param pageId the page
     * @return whether it was seen
     */
    public boolean frontPageSeen(String pageId) {
        if (frontPagesSeenInMemory != null) {
            return frontPagesSeenInMemory.contains(pageId);
        }
        return session != null && preferences().getBoolean(key("frontPageSeen." + pageId), false);
    }

    /**
     * Marks a front page as seen, once the player moved on from it.
     *
     * @param pageId the page
     */
    public void markFrontPageSeen(String pageId) {
        if (frontPagesSeenInMemory != null) {
            frontPagesSeenInMemory.add(pageId);
            return;
        }
        if (session == null || frontPageSeen(pageId)) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean(key("frontPageSeen." + pageId), true);
        preferences.flush();
    }

    /**
     * Keeps the front pages' seen marks in memory instead of the preferences, for screenshots.
     *
     * @param seen the pages to treat as seen; the set is changed as more are seen
     */
    public void useFrontPageMarksInMemory(java.util.Set<String> seen) {
        frontPagesSeenInMemory = seen;
        fillersInMemory = new java.util.HashMap<>();
    }

    /**
     * Returns the filler items a front page showed on this device ({@code ui-spec.md} &sect;4.18.1), so it shows
     * the same ones when it opens again.
     *
     * @param pageId the page
     * @return their ids in order, or {@code null} if it hasn't shown any yet
     */
    public java.util.List<String> fillersShown(String pageId) {
        String value = fillerValue("fillers." + pageId);
        return value == null ? null : ids(value);
    }

    /**
     * Returns the filler items shown on this device since every one had been, so none repeats before the rest.
     *
     * @return their ids
     */
    public java.util.Set<String> fillersSeen() {
        String value = fillerValue("fillersSeen");
        return value == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(ids(value));
    }

    /**
     * Records what a front page showed and the items shown since the deck last ran out.
     *
     * @param pageId the page
     * @param shown  its items, in order
     * @param seen   the items shown since the deck last ran out, this page's included
     */
    public void recordFillers(String pageId, java.util.List<String> shown, java.util.Set<String> seen) {
        if (fillersInMemory != null) {
            fillersInMemory.put("fillers." + pageId, String.join(",", shown));
            fillersInMemory.put("fillersSeen", String.join(",", seen));
            return;
        }
        if (session == null) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putString(key("fillers." + pageId), String.join(",", shown));
        preferences.putString(key("fillersSeen"), String.join(",", seen));
        preferences.flush();
    }

    private String fillerValue(String name) {
        if (fillersInMemory != null) {
            return fillersInMemory.get(name);
        }
        if (session == null) {
            return null;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        return preferences.contains(key(name)) ? preferences.getString(key(name)) : null;
    }

    private static java.util.List<String> ids(String value) {
        return value.isEmpty() ? java.util.List.of() : java.util.List.of(value.split(","));
    }

    /**
     * Returns whether this player has opened the help on this device, for the advisor
     * ({@code ui-spec.md} component 24).
     *
     * @return whether it was opened
     */
    public boolean helpOpened() {
        return flag("helpOpened");
    }

    /**
     * Marks the help as opened.
     */
    public void markHelpOpened() {
        setFlag("helpOpened");
    }

    /**
     * Returns whether the game plays sound, on this device for everyone (Settings, {@code ui-spec.md}
     * &sect;4.12): read before anyone logs in, by the splash.
     *
     * @return whether sound is on (the default)
     */
    public static boolean soundOn() {
        return soundInMemory != null ? soundInMemory : preferences().getBoolean("sound", true);
    }

    /**
     * Keeps the sound setting in memory from now on (screenshots), so it doesn't depend on this machine.
     *
     * @param on whether sound is on
     */
    public static void useSoundInMemory(boolean on) {
        soundInMemory = on;
    }

    /**
     * Turns the game's sound on or off on this device.
     *
     * @param on whether to play sound
     */
    public static void setSoundOn(boolean on) {
        if (soundInMemory != null) {
            soundInMemory = on;
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean("sound", on);
        preferences.flush();
    }

    /**
     * The Settings' text sizes ({@code ui-spec.md} &sect;4.12): the design's own, and a step larger for a
     * small, sharp screen, where the 1920 by 1080 layout's text comes out tiny (user, 2026-10-26).
     */
    public enum TextSize {
        /** The design's sizes. */
        STANDARD(1f),
        /** Every working style 12 % larger (the default); the drawings' labels and the newspaper keep their size. */
        LARGE(1.12f);

        private final float scale;

        TextSize(float scale) {
            this.scale = scale;
        }

        /**
         * Returns how much larger than the design the text is drawn.
         *
         * @return the factor
         */
        public float scale() {
            return scale;
        }
    }

    /**
     * Returns the text size on this device, for everyone (Settings).
     *
     * @return the text size; {@link TextSize#LARGE} by default (user, 2026-10-26)
     */
    public static TextSize textSize() {
        if (textSizeInMemory != null) {
            return textSizeInMemory;
        }
        try {
            return TextSize.valueOf(preferences().getString("textSize", TextSize.LARGE.name()));
        } catch (IllegalArgumentException e) {
            return TextSize.LARGE;
        }
    }

    /**
     * Keeps the text size in memory from now on (screenshots), so it doesn't depend on this machine.
     *
     * @param size the text size
     */
    public static void useTextSizeInMemory(TextSize size) {
        textSizeInMemory = size;
    }

    /**
     * Sets the text size on this device and redraws the text at once: the fonts are rebuilt, and every
     * screen lays its text out again when it sees them change.
     *
     * @param size the text size
     */
    public void setTextSize(TextSize size) {
        if (textSizeInMemory != null) {
            textSizeInMemory = size;
        } else {
            com.badlogic.gdx.Preferences preferences = preferences();
            preferences.putString("textSize", size.name());
            preferences.flush();
        }
        ui.fonts().setTextScale(size.scale());
    }

    /**
     * Returns the replay's music volume on this device (Settings).
     *
     * @return 0 to 1; 0.7 by default
     */
    public static float musicVolume() {
        return volume("musicVolume");
    }

    /**
     * Returns the replay's sound-effects volume on this device (Settings): the console, the clips'
     * cues and the room. The voices at the start and on joining play at full volume.
     *
     * @return 0 to 1; 0.7 by default
     */
    public static float effectsVolume() {
        return volume("effectsVolume");
    }

    /**
     * Sets the replay's music volume on this device.
     *
     * @param volume 0 to 1
     */
    public static void setMusicVolume(float volume) {
        setVolume("musicVolume", volume);
    }

    /**
     * Sets the replay's sound-effects volume on this device.
     *
     * @param volume 0 to 1
     */
    public static void setEffectsVolume(float volume) {
        setVolume("effectsVolume", volume);
    }

    private static float volume(String key) {
        if (soundInMemory != null) {
            return volumesInMemory.getOrDefault(key, DEFAULT_VOLUME);
        }
        return Math.max(0f, Math.min(1f, preferences().getFloat(key, DEFAULT_VOLUME)));
    }

    private static void setVolume(String key, float volume) {
        float clamped = Math.max(0f, Math.min(1f, volume));
        if (soundInMemory != null) {
            volumesInMemory.put(key, clamped);
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putFloat(key, clamped);
        preferences.flush();
    }

    /**
     * Plays an audio clip once, if sound is on.
     *
     * @param path the clip, under {@code assets/}
     * @return the playing clip, for its owner to dispose; {@code null} when silent or it won't load
     */
    public static com.badlogic.gdx.audio.Music playOnce(String path) {
        if (!soundOn()) {
            return null;
        }
        try {
            com.badlogic.gdx.audio.Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
            music.play();
            return music;
        } catch (RuntimeException e) {
            Gdx.app.error("Audio", "can't play " + path, e);
            return null;
        }
    }

    /**
     * Whether a new turn's missions play before its report ({@code ui-spec.md} &sect;4.4).
     */
    public enum ReplaysFirst {
        /** Ask each time (the default). */
        ASK,
        /** Always play them first. */
        ALWAYS,
        /** Never: the report opens as before. */
        NEVER
    }

    /**
     * Returns this player's choice on this device about watching a turn's missions before its report.
     * Under the in-memory marks it's the marks "replaysAlways" and "replaysNever".
     *
     * @return the choice
     */
    public ReplaysFirst replaysFirst() {
        if (flagsInMemory != null) {
            return flagsInMemory.contains("replaysAlways") ? ReplaysFirst.ALWAYS
                : flagsInMemory.contains("replaysNever") ? ReplaysFirst.NEVER : ReplaysFirst.ASK;
        }
        if (session == null) {
            return ReplaysFirst.ASK;
        }
        try {
            return ReplaysFirst.valueOf(preferences().getString(key("replaysFirst"), ReplaysFirst.ASK.name()));
        } catch (IllegalArgumentException e) {
            return ReplaysFirst.ASK;
        }
    }

    /**
     * Keeps this player's choice about watching a turn's missions first.
     *
     * @param choice the choice
     */
    public void setReplaysFirst(ReplaysFirst choice) {
        if (flagsInMemory != null) {
            flagsInMemory.remove("replaysAlways");
            flagsInMemory.remove("replaysNever");
            if (choice == ReplaysFirst.ALWAYS) {
                flagsInMemory.add("replaysAlways");
            } else if (choice == ReplaysFirst.NEVER) {
                flagsInMemory.add("replaysNever");
            }
            return;
        }
        if (session == null) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putString(key("replaysFirst"), choice.name());
        preferences.flush();
    }

    /**
     * Returns whether the advisor is retired for this player on this device: everything done, or
     * the player said they know the ropes.
     *
     * @return whether it's retired
     */
    public boolean advisorRetired() {
        return flag("advisorRetired");
    }

    /**
     * Returns whether this player has read a turn report, for the advisor's last item. Under the
     * in-memory marks it's the mark "reportRead", so screenshots don't depend on this machine.
     *
     * @return whether one was read
     */
    public boolean advisorReportRead() {
        if (flagsInMemory != null) {
            return flagsInMemory.contains("reportRead");
        }
        return lastReadReport() >= 1;
    }

    /**
     * Retires the advisor for good.
     */
    public void retireAdvisor() {
        setFlag("advisorRetired");
    }

    /**
     * Returns whether the advisor's idle tip is done for this player on this device: shown and acted
     * on, dismissed, or the advisor hidden ({@code Advisor.IDLE_TIP}).
     *
     * @return whether it's done
     */
    public boolean idleTipDone() {
        return flag("idleTipDone");
    }

    /**
     * Returns whether the advisor's idle tip has been shown.
     *
     * @return whether it has
     */
    public boolean idleTipShown() {
        return flag("idleTipShown");
    }

    /**
     * Marks the advisor's idle tip as shown.
     */
    public void markIdleTipShown() {
        setFlag("idleTipShown");
    }

    /**
     * Marks the advisor's idle tip as done: it never shows again.
     */
    public void markIdleTipDone() {
        setFlag("idleTipDone");
    }

    /**
     * Returns whether the advisor's card is minimized to its head. Kept for this client run only,
     * so a fresh start shows the whole card again.
     *
     * @return whether it's minimized
     */
    public boolean advisorMinimized() {
        return advisorMinimized;
    }

    /**
     * Minimizes the advisor's card to its head, or opens it again.
     *
     * @param minimized whether to minimize it
     */
    public void setAdvisorMinimized(boolean minimized) {
        advisorMinimized = minimized;
    }

    /**
     * Keeps the advisor's marks (help opened, advisor retired) in memory instead of the
     * preferences, so that screenshots don't depend on this machine.
     *
     * @param flags the marks set; the set is changed as more are set
     */
    public void useAdvisorMarksInMemory(java.util.Set<String> flags) {
        flagsInMemory = flags;
    }

    private boolean flag(String name) {
        if (flagsInMemory != null) {
            return flagsInMemory.contains(name);
        }
        return session != null && preferences().getBoolean(key(name), false);
    }

    private void setFlag(String name) {
        if (flagsInMemory != null) {
            flagsInMemory.add(name);
            return;
        }
        if (session == null || flag(name)) {
            return;
        }
        com.badlogic.gdx.Preferences preferences = preferences();
        preferences.putBoolean(key(name), true);
        preferences.flush();
    }

    private String readKey() {
        return key("reportRead");
    }

    private String key(String name) {
        Credentials credentials = session.credentials();
        return name + "." + credentials.address().host() + ":" + credentials.address().port() + "."
            + credentials.login();
    }

    private static com.badlogic.gdx.Preferences preferences() {
        return Gdx.app.getPreferences("launchwindow");
    }

    /**
     * Returns the hub's time-of-day clock, which lives as long as the client does.
     *
     * @return the clock
     */
    public Hub.Clock hubClock() {
        return hubClock;
    }

    /**
     * Returns the session, once the player is in the game.
     *
     * @return the session, or {@code null} before entering
     */
    public ClientSession session() {
        return session;
    }

    /**
     * Sets the session directly. For the offscreen screenshot tool, which builds screens without a
     * server.
     *
     * @param session the session
     */
    public void setSession(ClientSession session) {
        this.session = session;
    }

    /**
     * Returns the server address typed at the last connect, to prefill the Connect screen.
     *
     * @return the address text
     */
    public String lastServer() {
        return settings.server();
    }

    /**
     * Returns the login of the last successful login, to prefill the Connect screen.
     *
     * @return the login
     */
    public String lastLogin() {
        return settings.login();
    }

    /**
     * Returns the password of the last successful login, to prefill the Connect screen, so Enter
     * logs straight in. Kept in clear text in the player's home folder (user decision, 2026-10-05).
     *
     * @return the password
     */
    public String lastPassword() {
        return settings.password();
    }

    /**
     * Remembers a successful login's server, login and password, for this run and the next ones.
     * Only called once the server accepted them, so a mistyped password is never kept.
     *
     * @param server   the address text, as typed
     * @param login    the login
     * @param password the password
     */
    public void rememberConnect(String server, String login, String password) {
        settings = new ClientSettings(server, login, password);
        settingsStore.save(settings);
    }

    /**
     * Switches to a screen for a short visit, keeping the current one as it is (not disposed), so
     * the visited screen can go back to that very instance: help's explainer links come back to the
     * screen, help still open. The visited screen must end with {@code setScreen(previous)}.
     *
     * @param screen the screen to visit
     */
    public void visit(Screen screen) {
        super.setScreen(screen);
    }

    /**
     * Switches screens and disposes the previous one once the switch is done.
     *
     * @param screen the next screen
     */
    @Override
    public void setScreen(Screen screen) {
        Screen previous = getScreen();
        super.setScreen(screen);
        if (previous != null && previous != screen) {
            Gdx.app.postRunnable(previous::dispose);
        }
    }

    /**
     * Moves the glyphs' pixel grid to the new window size at once, and rebuilds the fonts for it
     * once the size has settled ({@link #render}).
     */
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (ui == null || width <= 0 || height <= 0) {
            return; // minimized
        }
        float[] scale = Theme.Fonts.windowScale(width, height);
        ui.fonts().setPixelGrid(scale[0], scale[1]);
        pendingFontScale = fontScale(scale[1]);
        lastResizeMillis = System.currentTimeMillis();
    }

    /**
     * Rebuilds the fonts once the window has kept a new size for {@value #FONT_REBUILD_DELAY_MS}
     * ms, so dragging a window's border doesn't rebuild them on every step. The screens notice
     * the rebuild ({@link Theme.Fonts#generation}) and lay their text out again.
     */
    @Override
    public void render() {
        if (ui != null && pendingFontScale != ui.fonts().scale()
            && System.currentTimeMillis() - lastResizeMillis >= FONT_REBUILD_DELAY_MS) {
            ui.fonts().regenerate(pendingFontScale);
        }
        super.render();
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        super.dispose();
        if (current != null) {
            current.dispose();
        }
        if (gateway != null) {
            gateway.dispose();
        }
        if (ui != null) {
            ui.dispose();
        }
    }

    /**
     * Picks the scale the fonts are generated at: the window's own pixels per layout unit, so each
     * glyph texel lands on one screen pixel. Clamped, which keeps the glyph pages small.
     *
     * @param windowScale the window's vertical pixels per layout unit
     * @return the scale, from {@value #MIN_FONT_SCALE} to {@value #MAX_FONT_SCALE}
     */
    private static float fontScale(float windowScale) {
        return Math.min(MAX_FONT_SCALE, Math.max(MIN_FONT_SCALE, windowScale));
    }
}
