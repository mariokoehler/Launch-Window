package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.content.ContentStrings;
import de.mkoehler.launchwindow.net.AdminPasswords;
import de.mkoehler.launchwindow.net.messages.StartNewGameRequest;
import de.mkoehler.launchwindow.net.messages.ServerSettings;
import de.mkoehler.launchwindow.net.messages.ServerSettingsResponse;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * <p>Server settings ({@code ui-spec.md} &sect;4.22, G21): the admin's editor for {@code game-meta.json}, behind
 * the admin password. Opened from Settings and from Join game, so it brings its own scrim, waiting chip and toast
 * rather than leaning on a session screen; everything it shows comes from the server's {@link ServerSettings}, so
 * {@code ScreenSnapshot} can render every state without a server.</p>
 *
 * <p>The password is kept in memory while the dialog is open and sent with every request; nothing about the unlock
 * is saved on the device.</p>
 *
 * @author Mario Koehler
 */
public final class ServerSettingsDialog {

    /** The unlock step's width, from the mockup. */
    static final float UNLOCK_WIDTH = 480f;
    /** The settings' width, from the mockup. */
    static final float WIDTH = 1100f;
    /** The section list's width. */
    private static final float NAV_WIDTH = 210f;
    /** The form's height: it scrolls inside. */
    private static final float SHELL_HEIGHT = 560f;
    /** A field's height. */
    private static final float FIELD = 34f;
    /** The time zone popover's rows. */
    private static final int ZONE_ROWS = 8;

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final List<String> ZONES = new ArrayList<>(new TreeSet<>(ZoneId.getAvailableZoneIds()));

    /** A section of the settings. */
    public enum Section {
        /** When turns resolve. */
        SCHEDULE("Schedule", "When turns resolve. Applies at once, from the next resolution."),
        /** The blackout periods. */
        BREAKS("Breaks", "Days on which no turn resolves, in the game's zone. Applies at once: a break covering "
            + "today stops today's turn if it hasn't resolved."),
        /** The pack, the goals, the seed. */
        GAME("Game", "The game itself. Pack and seed can change until the first agency is founded; the goals "
            + "until the first turn resolves."),
        /** The filler bots. */
        BOTS("Bots", "Agencies the server plays itself, filling the race. Names are optional, up to 32 characters."),
        /** Dev mode. */
        DEVELOPMENT("Development", "For play tests. Both apply at once."),
        /** The admin password. */
        ADMIN("Admin", "The password that opens these settings. The current one is never shown.");

        private final String title;
        private final String lead;

        Section(String title, String lead) {
            this.title = title;
            this.lead = lead;
        }

        /** The section a field's key belongs to. */
        static Section of(String key) {
            if (key.startsWith("breaks")) {
                return BREAKS;
            }
            if (key.startsWith("bots")) {
                return BOTS;
            }
            return switch (key) {
                case "pack", "goals", "seed" -> GAME;
                case "password", "repeat" -> ADMIN;
                default -> SCHEDULE;
            };
        }
    }

    /** What the unlock step shows. */
    enum UnlockState {
        /** Ready for the password. */
        READY,
        /** Waiting for the server. */
        WAITING,
        /** The password was wrong. */
        WRONG,
        /** Too many wrong tries. */
        LOCKED
    }

    /** Which popover is open. */
    private enum Popup {
        NONE, ZONE, PACK, CALENDAR
    }

    /** The descriptions of the bundled packs (unconfirmed). */
    private static final Map<String, String> PACK_DESCRIPTIONS = Map.of(
        "standard", "The whole race, from the first satellite to the Moon and the planets",
        "early-era", "The early years: the first satellites and the first people in space");

    private final LaunchWindowGame game;
    private final Stage stage;
    private final UiKit ui;
    private final Credentials credentials;
    private final Table scrim = new Table();
    private final Map<String, ContentStrings> packText = new HashMap<>();

    private String password = "";
    private UnlockState unlockState = UnlockState.READY;
    private String unlockMessage = "";
    private boolean showPassword;

    private ServerSettings saved;
    private Section section = Section.SCHEDULE;
    private final EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    private String time = "";
    private String zone = "";
    private final List<String[]> breaks = new ArrayList<>();
    private String pack = "";
    private final List<String> goals = new ArrayList<>();
    private String seed = "";
    private boolean devMode;
    private boolean archiveOpen;
    private final List<String[]> bots = new ArrayList<>();
    private String newPassword = "";
    private String repeatPassword = "";
    private String generated;
    private boolean showNew;

    private final Map<String, String> errors = new LinkedHashMap<>();
    private String footMessage = "";
    private boolean footDanger;
    private boolean stale;
    private boolean busy;
    private boolean discarding;
    private boolean confirmingNewGame;
    private String newGameTyped = "";
    private TextButton newGameButton;

    private Popup popup = Popup.NONE;
    private String zoneQuery = "";
    private String goalQuery = "";
    private int calendarRow;
    private boolean calendarEnd;
    private YearMonth calendarMonth;

    private Table navHolder;
    private TextButton saveButton;
    private ScrollPane formScroll;
    private Table zoneList;
    private Table goalList;
    private Table popupLayer;
    private Table waitingChip;

    /**
     * Prepares the dialog over a screen's stage.
     *
     * @param game        the game, for its gateway and UI kit
     * @param stage       the screen's stage
     * @param credentials who's logged in, and where
     */
    public ServerSettingsDialog(LaunchWindowGame game, Stage stage, Credentials credentials) {
        this.game = Objects.requireNonNull(game, "game must not be null");
        this.stage = Objects.requireNonNull(stage, "stage must not be null");
        this.ui = game.ui();
        this.credentials = credentials;
        scrim.setFillParent(true);
        scrim.setBackground(ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.55f)));
        scrim.setTouchable(Touchable.enabled);
        scrim.addListener(new ClickListener());
    }

    /**
     * Opens the unlock step.
     */
    public void open() {
        if (scrim.getStage() == null) {
            stage.addActor(scrim);
        }
        showUnlock();
    }

    /** Closes the dialog. */
    void close() {
        scrim.remove();
        password = "";
    }

    // ---------------------------------------------------------------------------------------------- unlock

    private void showUnlock() {
        scrim.clearChildren();
        Table card = card("Admin", "Server settings");
        card.add(ui.paragraph("Enter the admin password. The server prints it to its log at every start; it's stored "
            + "in game-meta.json as adminPassword.", Theme.TextStyle.BODY, Theme.INK)).padBottom(Theme.SPACE_4).row();
        card.add(fieldLabel("Admin password")).padBottom(Theme.SPACE_1).row();
        boolean locked = unlockState == UnlockState.LOCKED;
        TextField field = passwordField(locked ? "" : password, "Admin password", showPassword,
            unlockState == UnlockState.WRONG);
        field.setDisabled(locked);
        // A change listener, not only the key listener: a paste fires only the change.
        onChange(field, () -> password = field.getText());
        field.setTextFieldListener((f, c) -> {
            if (c == '\r' || c == '\n') {
                unlock();
            }
        });
        card.add(withEye(field, showPassword, () -> {
            password = field.getText();
            showPassword = !showPassword;
            showUnlock();
        })).height(FIELD).row();
        if (unlockState == UnlockState.WRONG || locked) {
            card.add(errorLine(unlockMessage)).padTop(Theme.SPACE_1).row();
        }
        Table buttons = new Table();
        buttons.add(ChromeScreen.onClick(ui.button("Cancel", Theme.ButtonKind.SECONDARY), this::close))
            .padRight(10f);
        TextButton unlock = ChromeScreen.onClick(ui.button("Unlock", Theme.ButtonKind.PRIMARY), this::unlock);
        unlock.setDisabled(locked || unlockState == UnlockState.WAITING);
        buttons.add(unlock);
        card.add(buttons).right().fill(false, false).padTop(Theme.SPACE_4).row();
        scrim.add(card).width(UNLOCK_WIDTH);
        addWaitingChip(unlockState == UnlockState.WAITING);
        stage.setKeyboardFocus(field);
        field.setCursorPosition(field.getText().length());
    }

    private void unlock() {
        if (unlockState == UnlockState.WAITING || unlockState == UnlockState.LOCKED) {
            return;
        }
        unlockState = UnlockState.WAITING;
        showUnlock();
        game.gateway().fetchServerSettings(credentials, password, this::unlocked);
    }

    private void unlocked(ServerSettingsResponse response) {
        switch (response.status()) {
            case OK -> {
                unlockState = UnlockState.READY;
                load(response.settings());
                section = Section.SCHEDULE;
                showSettings();
            }
            case WRONG_ADMIN_PASSWORD -> showUnlockState(UnlockState.WRONG, response.message());
            case ADMIN_LOCKED_OUT -> showUnlockState(UnlockState.LOCKED, response.message());
            default -> showUnlockState(UnlockState.WRONG, response.message().isEmpty()
                ? "The server couldn't answer." : response.message());
        }
    }

    /**
     * Shows the unlock step in a state, for the shots and the server's answers.
     *
     * @param state   the state
     * @param message the line under the field, for WRONG and LOCKED
     */
    void showUnlockState(UnlockState state, String message) {
        if (scrim.getStage() == null) {
            stage.addActor(scrim);
        }
        unlockState = state;
        unlockMessage = message == null ? "" : message;
        showUnlock();
    }

    /**
     * Shows the unlock step with a password typed, for the shots.
     *
     * @param state   the state
     * @param typed   the password in the field
     * @param shown   whether it's shown in clear
     * @param message the line under the field
     */
    public void showUnlockForShots(String state, String typed, boolean shown, String message) {
        password = typed;
        showPassword = shown;
        showUnlockState(UnlockState.valueOf(state), message);
    }

    // ---------------------------------------------------------------------------------------------- the model

    private void load(ServerSettings settings) {
        saved = settings;
        days.clear();
        for (String day : settings.activeDays()) {
            days.add(DayOfWeek.valueOf(day));
        }
        time = settings.resolutionTime();
        zone = settings.timezone();
        breaks.clear();
        for (ServerSettings.Break range : settings.breaks()) {
            breaks.add(new String[] {range.start(), range.end()});
        }
        pack = settings.contentPack();
        goals.clear();
        goals.addAll(settings.declaredGoalIds());
        seed = settings.gameSeed() == null ? "" : Long.toString(settings.gameSeed());
        devMode = settings.devMode();
        archiveOpen = settings.devArchiveOpen();
        bots.clear();
        for (ServerSettings.Bot bot : settings.bots()) {
            bots.add(new String[] {bot.id(), bot.archetype(), bot.agencyName(), bot.directorName(),
                Long.toString(bot.sinceTurn())});
        }
        newPassword = "";
        repeatPassword = "";
        generated = null;
        errors.clear();
        stale = false;
        popup = Popup.NONE;
    }

    private boolean dirty(Section which) {
        if (saved == null) {
            return false;
        }
        return switch (which) {
            case SCHEDULE -> !days.equals(savedDays()) || !time.equals(saved.resolutionTime())
                || !zone.equals(saved.timezone());
            case BREAKS -> !breakList().equals(saved.breaks());
            case GAME -> !pack.equals(saved.contentPack()) || !goals.equals(saved.declaredGoalIds())
                || !seed.equals(saved.gameSeed() == null ? "" : Long.toString(saved.gameSeed()));
            case BOTS -> !botList().equals(saved.bots());
            case DEVELOPMENT -> devMode != saved.devMode() || archiveOpen != saved.devArchiveOpen();
            case ADMIN -> !newPassword.isEmpty() || !repeatPassword.isEmpty();
        };
    }

    private boolean anyDirty() {
        for (Section which : Section.values()) {
            if (dirty(which)) {
                return true;
            }
        }
        return false;
    }

    private Set<DayOfWeek> savedDays() {
        EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        saved.activeDays().forEach(day -> set.add(DayOfWeek.valueOf(day)));
        return set;
    }

    private List<ServerSettings.Break> breakList() {
        return breaks.stream().map(b -> new ServerSettings.Break(b[0], b[1])).toList();
    }

    private List<ServerSettings.Bot> botList() {
        return bots.stream().map(b -> new ServerSettings.Bot(b[0], b[1], b[2], b[3], Long.parseLong(b[4]))).toList();
    }

    private boolean fixedBot(String[] bot) {
        return Long.parseLong(bot[4]) > 0;
    }

    private ServerSettings edited() {
        Long seedValue = saved.packLocked() || seed.isBlank() ? saved.gameSeed() : Long.valueOf(seed.strip());
        return new ServerSettings(saved.version(), saved.openTurn(), saved.agencies(), saved.nextResolution(),
            days.stream().map(DayOfWeek::name).toList(), time.strip(), zone.strip(), breakList(), pack, goals,
            seedValue, devMode, devMode && archiveOpen, botList(), saved.packLock(), saved.goalsLock(), List.of());
    }

    // ---------------------------------------------------------------------------------------------- settings

    /**
     * Shows the settings, for the shots: as the server sent them, on one section.
     *
     * @param settings the settings
     * @param which    the section
     */
    public void showSettingsForShots(ServerSettings settings, Section which) {
        if (scrim.getStage() == null) {
            stage.addActor(scrim);
        }
        load(settings);
        section = which;
        showSettings();
    }

    private void showSettings() {
        float scrollY = formScroll == null ? 0f : formScroll.getScrollY();
        boolean sameSection = formScroll != null && formScroll.getUserObject() == section;
        scrim.clearChildren();
        if (discarding) {
            scrim.add(discardCard()).width(SettingsDialog.WIDTH);
            return;
        }
        if (confirmingNewGame) {
            scrim.add(newGameCard()).width(SettingsDialog.WIDTH);
            addWaitingChip(busy);
            return;
        }
        Table card = card("Admin", "Server settings");
        Table status = new Table();
        Image clock = icon("clock", Theme.INK_MUTED);
        status.add(clock).size(14f).padRight(Theme.SPACE_2);
        status.add(ui.paragraph(statusLine(), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).growX().left();
        card.add(status).padBottom(Theme.SPACE_3).row();
        card.add(rule()).height(Theme.BORDER_THIN).row();

        Table shell = new Table();
        navHolder = new Table();
        navHolder.top();
        fillNav();
        shell.add(navHolder).width(NAV_WIDTH).growY().top();
        shell.add(new Image(ui.solid(Theme.NAVY_BORDER))).width(Theme.BORDER_THIN).growY();
        Table form = new Table();
        form.top().left();
        form.defaults().growX().left();
        form.pad(Theme.SPACE_1, 26f, Theme.SPACE_4, Theme.SPACE_3);
        if (stale) {
            form.add(staleBanner()).padBottom(18f).row();
        }
        Table head = new Table();
        head.defaults().growX().left();
        head.add(ui.label(section.title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(3f).row();
        String lead = section == Section.GAME && saved.packLocked()
            ? "The game itself: these are fixed now that it has begun." : section.lead;
        head.add(ui.paragraph(lead, Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).row();
        form.add(head).padBottom(18f).row();
        switch (section) {
            case SCHEDULE -> schedule(form);
            case BREAKS -> breaks(form);
            case GAME -> gameSection(form);
            case BOTS -> botsSection(form);
            case DEVELOPMENT -> development(form);
            case ADMIN -> admin(form);
        }
        formScroll = new ScrollPane(form, new ScrollPane.ScrollPaneStyle());
        formScroll.setFadeScrollBars(false);
        formScroll.setScrollingDisabled(true, false);
        formScroll.setUserObject(section);
        shell.add(formScroll).grow();
        card.add(shell).height(SHELL_HEIGHT).row();
        card.add(rule()).height(Theme.BORDER_THIN).row();

        Table foot = new Table();
        foot.add(ui.paragraph(footMessage, Theme.TextStyle.CAPTION, footDanger ? Theme.DANGER : Theme.INK_MUTED))
            .growX().left().padRight(Theme.SPACE_4);
        foot.add(ChromeScreen.onClick(ui.button("Cancel", Theme.ButtonKind.SECONDARY), this::cancel)).padRight(10f);
        saveButton = ChromeScreen.onClick(ui.button("Save", Theme.ButtonKind.PRIMARY), this::save);
        foot.add(saveButton);
        card.add(foot).padTop(14f).row();
        refreshChrome();
        scrim.add(card).width(WIDTH);

        popupLayer = new Table();
        popupLayer.setFillParent(true);
        popupLayer.setTouchable(Touchable.childrenOnly);
        scrim.addActor(popupLayer);
        addWaitingChip(busy);
        if (sameSection) {
            scrim.layout();
            formScroll.layout();
            formScroll.setScrollY(scrollY);
            formScroll.updateVisualScroll();
        }
        if (popup != Popup.NONE) {
            Gdx.app.postRunnable(this::showPopup);
        }
    }

    private String statusLine() {
        StringBuilder text = new StringBuilder("Turn ").append(saved.openTurn()).append(" open");
        if (saved.agencies() == 0) {
            text.append(" · no agency yet");
        }
        if (saved.nextResolution() < 0) {
            return text.append(" · no resolution scheduled").toString();
        }
        ZoneId gameZone;
        try {
            gameZone = ZoneId.of(saved.timezone());
        } catch (RuntimeException e) {
            gameZone = ZoneOffset.UTC;
        }
        Instant next = Instant.ofEpochMilli(saved.nextResolution());
        return text.append(" · next resolution ").append(WHEN.format(next.atZone(gameZone))).append(' ')
            .append(gameZone.getId()).append(" (").append(CLOCK.format(next.atZone(ZoneId.systemDefault())))
            .append(" your time)").toString();
    }

    /** Fills the section list: the active one barred, a dot for changes, a red mark for an error. */
    private void fillNav() {
        navHolder.clearChildren();
        navHolder.padTop(10f);
        for (Section which : Section.values()) {
            boolean on = which == section;
            Table item = new Table();
            item.pad(9f, 11f, 9f, 14f);
            item.add(ui.label(which.title, on ? Theme.TextStyle.BODY_STRONG : Theme.TextStyle.BODY,
                on ? Theme.NAVY : Theme.INK)).expandX().left();
            boolean error = errors.keySet().stream().anyMatch(key -> Section.of(key) == which);
            if (error) {
                item.add(errorMark()).size(15f);
            } else if (dirty(which)) {
                item.add(new Image(ui.circle(Theme.NAVY, Theme.NAVY, 0f, 8))).size(8f);
            }
            Table entry = barred(item, on);
            entry.setTouchable(Touchable.enabled);
            entry.addListener(SessionScreen.clicked(() -> {
                section = which;
                popup = Popup.NONE;
                showSettings();
            }));
            navHolder.add(entry).growX().padBottom(2f).row();
        }
    }

    /** Updates what changes as the admin types: the section marks, Save. */
    private void refreshChrome() {
        if (navHolder != null) {
            fillNav();
        }
        if (saveButton != null) {
            saveButton.setDisabled(busy || stale || !anyDirty());
        }
    }

    // ---------------------------------------------------------------------------------------------- schedule

    private void schedule(Table form) {
        form.add(fieldLabel("Days")).padBottom(Theme.SPACE_1).row();
        Table row = new Table();
        row.left();
        Table chips = new Table();
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean on = days.contains(day);
            Table chip = new Table();
            chip.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
            chip.add(ui.label(day.name().substring(0, 3), Theme.TextStyle.LABEL, on ? Theme.ON_FILL : Theme.NAVY));
            chip.setTouchable(Touchable.enabled);
            chip.addListener(SessionScreen.clicked(() -> {
                if (!days.remove(day)) {
                    days.add(day);
                }
                errors.remove("days");
                showSettings();
            }));
            chips.add(chip).size(50f, 36f).padRight(6f);
        }
        row.add(chips).padRight(10f);
        row.add(smallButton("Every day", () -> {
            days.addAll(EnumSet.allOf(DayOfWeek.class));
            errors.remove("days");
            showSettings();
        })).padRight(Theme.SPACE_2);
        row.add(smallButton("Weekdays", () -> {
            days.clear();
            days.addAll(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
            errors.remove("days");
            showSettings();
        }));
        form.add(row).padBottom(Theme.SPACE_1).row();
        form.add(errorOrHint("days", "At least one day. " + turnsAWeek(days.size()))).padBottom(18f).row();

        form.add(fieldLabel("Resolution time")).padBottom(Theme.SPACE_1).row();
        Table timeRow = new Table();
        timeRow.left();
        TextField timeField = field(time, "HH:MM", errors.containsKey("time"), text -> {
            time = text;
            errors.remove("time");
        });
        timeField.setMaxLength(5);
        timeRow.add(timeField).width(120f).height(FIELD).padRight(Theme.SPACE_3);
        timeRow.add(ui.label("24 h, in the zone below", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        form.add(timeRow).padBottom(errors.containsKey("time") ? Theme.SPACE_1 : 18f).row();
        if (errors.containsKey("time")) {
            form.add(errorLine(errors.get("time"))).padBottom(18f).row();
        }

        form.add(fieldLabel("Time zone")).padBottom(Theme.SPACE_1).row();
        Table select = selectBox(zone, offset(zone), errors.containsKey("timezone"), () -> {
            popup = Popup.ZONE;
            zoneQuery = "";
            showPopup();
        });
        select.setName("zone");
        form.add(select).width(380f).height(FIELD).padBottom(Theme.SPACE_1).row();
        form.add(errorOrHint("timezone", "Turns resolve on these days, at this time, in this zone.")).row();
    }

    private static String turnsAWeek(int count) {
        String[] words = {"No", "One turn", "Two turns", "Three turns", "Four turns", "Five turns", "Six turns",
            "Seven turns"};
        return count == 0 ? "No turns at all." : words[count] + " a week.";
    }

    private static String offset(String zoneId) {
        try {
            ZoneOffset offset = ZoneId.of(zoneId).getRules().getOffset(Instant.now());
            return "UTC" + (offset.getTotalSeconds() == 0 ? "+00:00" : offset.getId());
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ---------------------------------------------------------------------------------------------- breaks

    private void breaks(Table form) {
        Table head = breakGrid();
        head.add(fieldLabel("From")).left();
        head.add(fieldLabel("To (inclusive)")).left();
        head.add(fieldLabel("Days")).left();
        head.add().growX();
        head.add();
        form.add(head).padBottom(Theme.SPACE_1).row();
        LocalDate today = today();
        for (int i = 0; i < breaks.size(); i++) {
            int index = i;
            String[] range = breaks.get(i);
            String key = "breaks." + i;
            LocalDate start = parse(range[0]);
            LocalDate end = parse(range[1]);
            boolean past = end != null && end.isBefore(today);
            Table row = breakGrid();
            for (int side = 0; side < 2; side++) {
                int which = side;
                Stack date = dateField(range[side], errors.containsKey(key) && (side == 1 || start == null),
                    text -> {
                        range[which] = text;
                        errors.remove(key);
                    }, () -> openCalendar(index, which == 1));
                date.setName("date-" + index + "-" + side);
                row.add(date).height(FIELD);
            }
            String count = start != null && end != null && !start.isAfter(end)
                ? (ChronoUnit.DAYS.between(start, end) + 1) + (start.equals(end) ? " day" : " days") : "–";
            row.add(ui.label(count, Theme.TextStyle.BODY_SM, Theme.INK)).left();
            Table tag = new Table();
            tag.left();
            if (past) {
                tag.add(tag("Past"));
            }
            row.add(tag).growX().left();
            row.add(iconButton("trash", "Remove the break", () -> {
                breaks.remove(index);
                errors.keySet().removeIf(k -> k.startsWith("breaks."));
                showSettings();
            })).size(32f);
            Table holder = new Table();
            holder.defaults().growX();
            holder.add(row).row();
            if (errors.containsKey(key)) {
                holder.add(errorLine(errors.get(key))).padLeft(182f).padTop(Theme.SPACE_1).row();
            }
            if (past) {
                holder.getColor().a = 0.55f;
            }
            holder.padTop(Theme.SPACE_2).padBottom(Theme.SPACE_2);
            form.add(holder).row();
            form.add(rule()).height(Theme.BORDER_THIN).row();
        }
        Table add = new Table();
        add.left();
        add.add(iconTextButton("plus", "Add break", () -> {
            String tomorrow = today().plusDays(1).toString();
            breaks.add(new String[] {tomorrow, tomorrow});
            showSettings();
        })).padRight(Theme.SPACE_3);
        add.add(ui.paragraph("A new break starts tomorrow; type the dates as YYYY-MM-DD or pick them.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        form.add(add).padTop(Theme.SPACE_3).row();
    }

    private Table breakGrid() {
        Table grid = new Table();
        grid.defaults().padRight(Theme.SPACE_3);
        grid.columnDefaults(0).width(170f);
        grid.columnDefaults(1).width(170f);
        grid.columnDefaults(2).width(90f);
        grid.columnDefaults(4).width(32f).padRight(0f);
        return grid;
    }

    private LocalDate today() {
        try {
            return LocalDate.now(ZoneId.of(zone));
        } catch (RuntimeException e) {
            return LocalDate.now();
        }
    }

    private static LocalDate parse(String text) {
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private void openCalendar(int row, boolean end) {
        calendarRow = row;
        calendarEnd = end;
        LocalDate date = parse(breaks.get(row)[end ? 1 : 0]);
        calendarMonth = YearMonth.from(date == null ? today() : date);
        popup = Popup.CALENDAR;
        showPopup();
    }

    /**
     * Opens a break's calendar, for the shots.
     *
     * @param row the break
     * @param end whether its end
     */
    public void openCalendarForShots(int row, boolean end) {
        scrim.layout();
        openCalendar(row, end);
    }

    // ---------------------------------------------------------------------------------------------- game

    private void gameSection(Table form) {
        boolean locked = saved.packLocked();
        form.add(fieldLabel("Content pack")).padBottom(Theme.SPACE_1).row();
        if (locked) {
            form.add(lockedBox(pack)).width(380f).height(FIELD).padBottom(Theme.SPACE_1).row();
        } else {
            Table select = selectBox(pack, "", errors.containsKey("pack"), () -> {
                popup = Popup.PACK;
                showPopup();
            });
            select.setName("pack");
            form.add(select).width(380f).height(FIELD).padBottom(Theme.SPACE_1).row();
        }
        form.add(ui.paragraph(PACK_DESCRIPTIONS.getOrDefault(pack, ""), Theme.TextStyle.CAPTION, Theme.INK_MUTED))
            .padBottom(Theme.SPACE_1).row();
        if (locked) {
            form.add(lockLine(saved.packLock())).padBottom(18f).row();
        } else {
            form.add(errorOrHint("pack", "")).padBottom(14f).row();
        }

        form.add(fieldLabel("Declared goals: the race is won by these")).padBottom(Theme.SPACE_2).row();
        boolean goalsLocked = saved.goalsLocked();
        Table picked = new Table();
        picked.left();
        float rowWidth = 0f;
        for (String goal : goals) {
            Table chip = new Table();
            chip.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            chip.pad(4f, 4f, 4f, goalsLocked ? 10f : 6f);
            Image patch = patch(goal);
            chip.add(patch).size(34f).padRight(Theme.SPACE_2);
            chip.add(ui.label(goalName(goal), Theme.TextStyle.BODY_SM, Theme.INK));
            if (!goalsLocked) {
                chip.add(iconButton("close", "Untick " + goalName(goal), () -> {
                    goals.remove(goal);
                    errors.remove("goals");
                    showSettings();
                })).size(24f).padLeft(Theme.SPACE_1);
            }
            chip.pack();
            if (rowWidth > 0 && rowWidth + chip.getPrefWidth() > 760f) {
                picked.row();
                rowWidth = 0f;
            }
            picked.add(chip).padRight(14f).padBottom(Theme.SPACE_2).left();
            rowWidth += chip.getPrefWidth() + 14f;
        }
        form.add(picked).padBottom(Theme.SPACE_1).row();
        if (goalsLocked) {
            form.add(lockLine(saved.goalsLock())).padBottom(18f).row();
        } else {
            if (errors.containsKey("goals")) {
                form.add(errorLine(errors.get("goals"))).padBottom(Theme.SPACE_2).row();
            }
            TextField search = ui.searchField("Find a goal");
            search.setText(goalQuery);
            search.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    goalQuery = search.getText();
                    fillGoalList();
                }
            });
            form.add(searchStack(search)).width(380f).height(FIELD).padBottom(Theme.SPACE_2).row();
            goalList = new Table();
            goalList.top().left();
            fillGoalList();
            ScrollPane list = new ScrollPane(goalList, new ScrollPane.ScrollPaneStyle());
            list.setFadeScrollBars(false);
            list.setScrollingDisabled(true, false);
            Table frame = new Table();
            frame.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            frame.pad(Theme.BORDER_THIN);
            frame.add(list).grow();
            form.add(frame).height(196f).padBottom(Theme.SPACE_1).row();
            ServerSettings.Pack chosen = saved.pack(pack);
            int total = chosen == null ? 0 : chosen.goalIds().size();
            form.add(ui.label(goals.size() + " of " + total + " picked · the pack's order", Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).padBottom(18f).row();
        }

        form.add(fieldLabel("Game seed")).padBottom(Theme.SPACE_1).row();
        if (locked) {
            form.add(lockedBox("Hidden once the game has begun")).width(380f).height(FIELD).padBottom(Theme.SPACE_1)
                .row();
            form.add(lockLine(saved.packLock())).row();
        } else {
            Table seedRow = new Table();
            seedRow.left();
            TextField seedField = field(seed, "", errors.containsKey("seed"), text -> {
                seed = text;
                errors.remove("seed");
            });
            seedField.setTextFieldFilter((f, c) -> Character.isDigit(c) || c == '-');
            seedRow.add(seedField).width(240f).height(FIELD).padRight(Theme.SPACE_3);
            seedRow.add(iconTextButton("dice", "Random", () -> {
                seed = Long.toString(new java.security.SecureRandom().nextLong() & Long.MAX_VALUE);
                errors.remove("seed");
                showSettings();
            }));
            form.add(seedRow).padBottom(Theme.SPACE_1).row();
            form.add(errorOrHint("seed", "Every turn's draws come from it.")).row();
        }
    }

    private void fillGoalList() {
        goalList.clearChildren();
        ServerSettings.Pack chosen = saved.pack(pack);
        if (chosen == null) {
            return;
        }
        String query = goalQuery.strip().toLowerCase(Locale.ROOT);
        for (String goal : chosen.goalIds()) {
            String name = goalName(goal);
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)
                    && !goal.contains(query)) {
                continue;
            }
            boolean on = goals.contains(goal);
            Table row = new Table();
            row.pad(5f, 10f, 5f, 10f);
            Table box = new Table();
            box.setBackground(ui.rect(on ? Theme.NAVY : Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
            if (on) {
                box.add(icon("check", Theme.ON_FILL)).size(11f);
            }
            row.add(box).size(16f).padRight(Theme.SPACE_3);
            row.add(patch(goal)).size(28f).padRight(Theme.SPACE_3);
            row.add(ui.label(name, Theme.TextStyle.BODY_SM, Theme.INK)).expandX().left();
            row.setTouchable(Touchable.enabled);
            row.addListener(SessionScreen.clicked(() -> {
                if (!goals.remove(goal)) {
                    // The pack's order, so the chips read as the race does.
                    goals.add(goal);
                    goals.sort(java.util.Comparator.comparingInt(chosen.goalIds()::indexOf));
                }
                errors.remove("goals");
                showSettings();
            }));
            goalList.add(row).growX().row();
            goalList.add(rule()).height(Theme.BORDER_THIN).growX().row();
        }
    }

    private String goalName(String goal) {
        ContentStrings strings = packText.computeIfAbsent(pack, id -> {
            try {
                return ContentStrings.loadFromClasspath("content/" + id, ServerSettingsDialog.class.getClassLoader());
            } catch (RuntimeException e) {
                return ContentStrings.EMPTY;
            }
        });
        ContentStrings.Text text = strings.goals().get(goal);
        return text == null || text.name() == null ? goal : text.name();
    }

    private Image patch(String goal) {
        Drawable art = ui.picture("patches/" + goal + ".png");
        Image image = art == null ? new Image(ui.circle(Theme.SKILL_TRACK, Theme.NAVY_BORDER, 1f, 34))
            : new Image(art, Scaling.fit);
        return image;
    }

    // ---------------------------------------------------------------------------------------------- bots

    private void botsSection(Table form) {
        Table head = botGrid();
        head.add(fieldLabel("Bot")).left();
        head.add(fieldLabel("Side")).left();
        head.add(fieldLabel("Agency name")).left();
        head.add(fieldLabel("Director")).left();
        head.add();
        form.add(head).padBottom(6f).row();
        for (int i = 0; i < bots.size(); i++) {
            int index = i;
            String[] bot = bots.get(i);
            boolean fixed = fixedBot(bot);
            String key = "bots." + i;
            Table row = botGrid();
            row.add(ui.label(bot[0], Theme.TextStyle.BODY_STRONG, Theme.INK)).left();
            row.add(segmented(new String[] {"NASA", "Soviet"}, "SOVIET".equals(bot[1]) ? 1 : 0, !fixed, choice -> {
                bot[1] = choice == 0 ? "NASA" : "SOVIET";
                showSettings();
            })).height(32f);
            if (fixed) {
                row.add(lockedBox(bot[2].isEmpty() ? "Drawn at random" : bot[2])).height(FIELD);
                row.add(lockedBox(bot[3].isEmpty() ? "Drawn at random" : bot[3])).height(FIELD);
                row.add();
            } else {
                row.add(field(bot[2], "Drawn at random", errors.containsKey(key + ".agency"), text -> {
                    bot[2] = text;
                    errors.remove(key + ".agency");
                })).height(FIELD);
                row.add(field(bot[3], "Drawn at random", errors.containsKey(key + ".director"), text -> {
                    bot[3] = text;
                    errors.remove(key + ".director");
                })).height(FIELD);
                row.add(iconButton("trash", "Remove " + bot[0], () -> {
                    bots.remove(index);
                    errors.keySet().removeIf(k -> k.startsWith("bots."));
                    showSettings();
                })).size(32f);
            }
            Table holder = new Table();
            holder.defaults().growX();
            holder.add(row).row();
            if (fixed) {
                holder.add(lockLine("In the race since turn " + bot[4] + ". Its agency stays: nobody else would "
                    + "play it.")).padLeft(74f).padTop(Theme.SPACE_1).row();
            }
            for (String field : List.of(".id", ".side", ".agency", ".director")) {
                if (errors.containsKey(key + field)) {
                    holder.add(errorLine(errors.get(key + field))).padLeft(74f).padTop(Theme.SPACE_1).row();
                }
            }
            holder.padTop(Theme.SPACE_2).padBottom(Theme.SPACE_2);
            form.add(holder).row();
            form.add(rule()).height(Theme.BORDER_THIN).row();
        }
        Table add = new Table();
        add.left();
        add.add(iconTextButton("plus", "Add bot", () -> {
            bots.add(new String[] {nextBotId(), "NASA", "", "", "0"});
            showSettings();
        })).padRight(Theme.SPACE_3);
        long fresh = bots.stream().filter(b -> !fixedBot(b)).count();
        String note = fresh == 1 && bots.stream().anyMatch(this::fixedBot)
            ? bots.stream().filter(b -> !fixedBot(b)).findFirst().orElseThrow()[0]
                + " founds its agency when you save, and plays from turn " + saved.openTurn() + "."
            : "Each founds its agency when you save, and plays from the next resolution.";
        add.add(ui.paragraph(note, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        form.add(add).padTop(Theme.SPACE_3).row();
    }

    private Table botGrid() {
        Table grid = new Table();
        grid.defaults().padRight(10f);
        grid.columnDefaults(0).width(64f);
        grid.columnDefaults(1).width(170f);
        grid.columnDefaults(2).growX();
        grid.columnDefaults(3).growX();
        grid.columnDefaults(4).width(32f).padRight(0f);
        return grid;
    }

    private String nextBotId() {
        Set<String> taken = new java.util.HashSet<>();
        bots.forEach(bot -> taken.add(bot[0]));
        int n = 1;
        while (taken.contains("bot-" + n)) {
            n++;
        }
        return "bot-" + n;
    }

    // ---------------------------------------------------------------------------------------------- development

    private void development(Table form) {
        form.add(fieldLabel("Dev mode")).padBottom(Theme.SPACE_1).row();
        Table devRow = new Table();
        devRow.left();
        devRow.add(segmented(new String[] {"Off", "On"}, devMode ? 1 : 0, true, choice -> {
            devMode = choice == 1;
            showSettings();
        })).width(160f).height(32f);
        form.add(devRow).padBottom(Theme.SPACE_2).row();
        form.add(ui.notice(Theme.NoticeKind.WARNING, "Every player gets Resolve turn now and the funds cheat. Use it "
            + "for play tests only.")).padBottom(18f).row();
        form.add(fieldLabel("Open every archive page")).padBottom(Theme.SPACE_1).row();
        Table archiveRow = new Table();
        archiveRow.left();
        archiveRow.add(segmented(new String[] {"Off", "On"}, devMode && archiveOpen ? 1 : 0, devMode, choice -> {
            archiveOpen = choice == 1;
            showSettings();
        })).width(160f).height(32f);
        form.add(archiveRow).padBottom(Theme.SPACE_1).row();
        form.add(ui.paragraph((devMode ? "" : "Needs dev mode. ") + "For reviewing the archive; saves nothing.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).row();
    }

    // ---------------------------------------------------------------------------------------------- admin

    private void admin(Table form) {
        Table fields = new Table();
        fields.top().left();
        fields.defaults().top().left();
        Table first = new Table();
        first.defaults().growX().left();
        first.add(fieldLabel("New password")).padBottom(Theme.SPACE_1).row();
        TextField fresh = passwordField(newPassword, "", showNew, errors.containsKey("password"));
        onChange(fresh, () -> {
            newPassword = fresh.getText();
            errors.remove("password");
            refreshChrome();
        });
        first.add(withEye(fresh, showNew, () -> {
            showNew = !showNew;
            showSettings();
        })).height(FIELD).padBottom(Theme.SPACE_1).row();
        first.add(errorOrHint("password", "At least " + AdminPasswords.MIN_LENGTH + " characters.")).row();
        Table second = new Table();
        second.defaults().growX().left();
        second.add(fieldLabel("Repeat")).padBottom(Theme.SPACE_1).row();
        TextField repeat = passwordField(repeatPassword, "", showNew, errors.containsKey("repeat"));
        onChange(repeat, () -> {
            repeatPassword = repeat.getText();
            errors.remove("repeat");
            refreshChrome();
        });
        second.add(repeat).height(FIELD).padBottom(Theme.SPACE_1).row();
        second.add(errorOrHint("repeat", "")).row();
        fields.add(first).width(280f).padRight(Theme.SPACE_4);
        fields.add(second).width(280f);
        form.add(fields).padBottom(Theme.SPACE_3).row();
        Table make = new Table();
        make.left();
        make.add(iconTextButton("spark", "Make a new one", () -> {
            generated = AdminPasswords.generate();
            newPassword = generated;
            repeatPassword = generated;
            errors.remove("password");
            errors.remove("repeat");
            showSettings();
        })).padRight(Theme.SPACE_3);
        make.add(ui.paragraph("Leave both empty to keep the current password.", Theme.TextStyle.CAPTION,
            Theme.INK_MUTED)).growX();
        form.add(make).padBottom(18f).row();
        if (generated != null && generated.equals(newPassword)) {
            Table box = new Table();
            box.setBackground(ui.noticeBackground(Theme.NoticeKind.WARNING));
            box.pad(10f, 12f, 10f, 12f);
            Table text = new Table();
            text.defaults().left();
            text.add(fieldLabel("The new password, shown once")).padBottom(2f).row();
            text.add(ui.label(generated, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(2f).row();
            text.add(ui.paragraph("Copy it now. It takes effect when you save; the server never sends it back.",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
            box.add(text).growX().left().padRight(Theme.SPACE_3);
            String shown = generated;
            box.add(iconTextButton("copy", "Copy", () -> {
                Gdx.app.getClipboard().setContents(shown);
                toast("Copied.", Theme.NoticeKind.SUCCESS);
            }));
            form.add(box).width(576f).row();
        }
        newGameBlock(form);
    }

    // ---------------------------------------------------------------------------------------------- new game

    /** The Admin section's last block: ending the game for everyone and starting a fresh one. */
    private void newGameBlock(Table form) {
        form.add(rule()).height(Theme.BORDER_THIN).padTop(Theme.SPACE_4).padBottom(18f).row();
        form.add(fieldLabel("Start a new game")).padBottom(Theme.SPACE_1).row();
        form.add(ui.paragraph("Ends this game for everyone and starts a fresh one with a new seed. Accounts and these "
            + "settings stay.", Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).padBottom(Theme.SPACE_2).row();
        Table row = new Table();
        row.left();
        boolean blocked = busy || stale || anyDirty();
        TextButton start = ChromeScreen.onClick(ui.button("Start a new game…", Theme.ButtonKind.DESTRUCTIVE), () -> {
            if (!busy && !stale && !anyDirty()) {
                confirmingNewGame = true;
                newGameTyped = "";
                errors.remove("confirm");
                footMessage = "";
                popup = Popup.NONE;
                showSettings();
            }
        });
        start.setDisabled(blocked);
        row.add(start).padRight(Theme.SPACE_3);
        if (blocked && !busy) {
            row.add(ui.paragraph(stale ? "Reload first." : "Save or discard your changes first.",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX();
        }
        form.add(row).row();
    }

    private Table newGameCard() {
        Table card = card("Danger", "End this game and start a new one?");
        card.add(ui.paragraph("Everyone's game ends now, and it can't be undone from here.",
            Theme.TextStyle.BODY_STRONG, Theme.DANGER)).padBottom(Theme.SPACE_3).row();
        String agencies = saved.agencies() == 1 ? "its 1 agency" : "all " + saved.agencies() + " of its agencies";
        String[][] lines = {
            {"Deleted", "the game at turn " + saved.openTurn() + ": " + agencies + ", bots included, with every "
                + "turn report, front page, standing chart and the game's result, and its turn backups."},
            {"Kept", "every account and its password, these settings and the admin password."},
            {"Then", "everyone online goes back to Connect and founds a new agency. The new game has a new random seed "
                + "and its first turn resolves on the schedule."},
            {"Saved", "a zip of the old game, in the server's old-games folder, to put back by hand if this was a "
                + "mistake."},
        };
        for (String[] line : lines) {
            Table entry = new Table();
            entry.add(ui.label(line[0].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED)).width(70f)
                .top().left().padTop(2f);
            entry.add(ui.paragraph(line[1], Theme.TextStyle.BODY_SM, Theme.INK)).growX().left();
            card.add(entry).padBottom(Theme.SPACE_2).row();
        }
        card.add(fieldLabel("Type " + StartNewGameRequest.CONFIRMATION + " to confirm")).padTop(Theme.SPACE_2)
            .padBottom(Theme.SPACE_1).row();
        TextField typed = field(newGameTyped, StartNewGameRequest.CONFIRMATION, errors.containsKey("confirm"),
            text -> {
                newGameTyped = text;
                errors.remove("confirm");
                if (newGameButton != null) {
                    newGameButton.setDisabled(busy || !confirmed());
                }
            });
        typed.setDisabled(busy);
        card.add(typed).width(260f).height(FIELD).padBottom(Theme.SPACE_1).row();
        card.add(errorOrHint("confirm", footDanger ? footMessage : "Capitals, as shown."))
            .padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(ChromeScreen.onClick(ui.button("Keep this game", Theme.ButtonKind.SECONDARY), () -> {
            if (!busy) {
                confirmingNewGame = false;
                footMessage = "";
                showSettings();
            }
        })).padRight(10f);
        newGameButton = ChromeScreen.onClick(ui.button("End it and start a new game", Theme.ButtonKind.DESTRUCTIVE),
            this::startNewGame);
        newGameButton.setDisabled(busy || !confirmed());
        buttons.add(newGameButton);
        card.add(buttons).right().fill(false, false).row();
        Gdx.app.postRunnable(() -> stage.setKeyboardFocus(typed));
        return card;
    }

    private boolean confirmed() {
        return StartNewGameRequest.CONFIRMATION.equals(newGameTyped.strip());
    }

    private void startNewGame() {
        if (busy || !confirmed()) {
            return;
        }
        busy = true;
        footMessage = "";
        footDanger = false;
        showSettings();
        game.gateway().startNewGame(credentials, password, saved.version(), newGameTyped.strip(), this::newGameAnswer);
    }

    private void newGameAnswer(ServerSettingsResponse response) {
        busy = false;
        switch (response.status()) {
            case OK -> {
                // The game is gone for this client too: back to Connect, saying so (also on the server's notice).
                close();
                game.leaveForNewGame(credentials);
            }
            case INVALID_FIELD -> {
                errors.putAll(response.fieldErrors());
                showSettings();
            }
            case STALE_SETTINGS -> {
                confirmingNewGame = false;
                showStale();
            }
            case WRONG_ADMIN_PASSWORD, ADMIN_LOCKED_OUT -> {
                confirmingNewGame = false;
                saved = null;
                showUnlockState(response.status() == ServerSettingsResponse.Status.ADMIN_LOCKED_OUT
                    ? UnlockState.LOCKED : UnlockState.WRONG, response.message());
            }
            default -> {
                footMessage = "The new game didn't start: " + (response.message().isEmpty()
                    ? "the server couldn't answer." : response.message());
                footDanger = true;
                showSettings();
            }
        }
    }

    /**
     * Opens the new game's confirmation, with some text typed, for the shots.
     *
     * @param typedText what the admin has typed
     */
    public void confirmNewGameForShots(String typedText) {
        confirmingNewGame = true;
        newGameTyped = typedText;
        showSettings();
    }

    // ---------------------------------------------------------------------------------------------- saving

    private void save() {
        if (busy || stale || !anyDirty()) {
            return;
        }
        popup = Popup.NONE;
        errors.clear();
        if (!newPassword.equals(repeatPassword)) {
            errors.put("repeat", "The passwords don't match.");
        }
        if (!saved.packLocked() && !seed.isBlank()) {
            try {
                Long.parseLong(seed.strip());
            } catch (NumberFormatException e) {
                errors.put("seed", "A whole number.");
            }
        }
        if (!errors.isEmpty()) {
            refused();
            return;
        }
        busy = true;
        footMessage = "";
        showSettings();
        game.gateway().saveServerSettings(credentials, password, edited(), newPassword, this::savedAnswer);
    }

    private void savedAnswer(ServerSettingsResponse response) {
        busy = false;
        switch (response.status()) {
            case OK -> {
                String changedPassword = newPassword;
                load(response.settings());
                if (!changedPassword.isEmpty()) {
                    password = changedPassword;
                }
                footMessage = "Saved. The next resolution is computed again from the new schedule.";
                footDanger = false;
                showSettings();
                toast("Server settings saved.", Theme.NoticeKind.SUCCESS);
            }
            case INVALID_FIELD -> {
                errors.putAll(response.fieldErrors());
                refused();
            }
            case STALE_SETTINGS -> showStale();
            case WRONG_ADMIN_PASSWORD, ADMIN_LOCKED_OUT -> {
                saved = null;
                showUnlockState(response.status() == ServerSettingsResponse.Status.ADMIN_LOCKED_OUT
                    ? UnlockState.LOCKED : UnlockState.WRONG, response.message());
            }
            default -> {
                footMessage = "Nothing was saved: " + (response.message().isEmpty() ? "the server couldn't answer."
                    : response.message());
                footDanger = true;
                showSettings();
            }
        }
    }

    private void refused() {
        footMessage = "Nothing was saved. Fix the marked field and save again.";
        footDanger = true;
        section = Section.of(errors.keySet().iterator().next());
        showSettings();
    }

    /**
     * Shows a refused save, for the shots: the server's words under the fields.
     *
     * @param refused the fields and their words
     */
    public void showRefusedForShots(Map<String, String> refused) {
        errors.clear();
        errors.putAll(refused);
        refused();
    }

    private void showStale() {
        stale = true;
        footMessage = "Save is off until you reload.";
        footDanger = false;
        showSettings();
    }

    /** Shows the stale banner, for the shots. */
    public void showStaleForShots() {
        showStale();
    }

    /** Shows the state after a save, for the shots. */
    public void showSavedForShots() {
        footMessage = "Saved. The next resolution is computed again from the new schedule.";
        footDanger = false;
        showSettings();
        toast("Server settings saved.", Theme.NoticeKind.SUCCESS);
    }

    private Table staleBanner() {
        Table banner = new Table();
        banner.setBackground(ui.noticeBackground(Theme.NoticeKind.WARNING));
        banner.pad(10f, 12f, 10f, 12f);
        banner.add(ui.paragraph("The settings changed on the server since you opened them. Reload to see them; your "
            + "changes are dropped.", Theme.TextStyle.BODY_SM, Theme.GOLD_TEXT)).growX().padRight(14f);
        banner.add(ChromeScreen.onClick(ui.button("Reload", Theme.ButtonKind.PRIMARY), this::reload));
        return banner;
    }

    private void reload() {
        busy = true;
        showSettings();
        game.gateway().fetchServerSettings(credentials, password, response -> {
            busy = false;
            if (response.status() == ServerSettingsResponse.Status.OK) {
                load(response.settings());
                footMessage = "";
                showSettings();
            } else {
                unlocked(response);
            }
        });
    }

    private void cancel() {
        if (anyDirty() && !busy) {
            discarding = true;
            popup = Popup.NONE;
            showSettings();
        } else {
            close();
        }
    }

    /**
     * Shows the question on Cancel with changes, for the shots.
     */
    public void showDiscardForShots() {
        discarding = true;
        showSettings();
    }

    /**
     * Changes a few settings as the admin would, for the shots: the Schedule's and the Breaks' first values.
     */
    public void editForShots() {
        if (!days.remove(DayOfWeek.SUNDAY)) {
            days.add(DayOfWeek.SUNDAY);
        }
        if (!breaks.isEmpty()) {
            breaks.get(breaks.size() - 1)[1] = breaks.get(breaks.size() - 1)[0];
        }
    }

    /**
     * Adds a bot as Add bot would, with names, for the shots.
     *
     * @param agency   its agency, or empty
     * @param director its director, or empty
     * @param side     NASA or SOVIET
     */
    public void addBotForShots(String agency, String director, String side) {
        bots.add(new String[] {nextBotId(), side, agency, director, "0"});
        showSettings();
    }

    /**
     * Makes a new admin password as "Make a new one" would, for the shots.
     *
     * @param made the password
     */
    public void makePasswordForShots(String made) {
        generated = made;
        newPassword = made;
        repeatPassword = made;
        showSettings();
    }

    /**
     * Opens the time zone search with some text typed, for the shots.
     *
     * @param query the text
     */
    public void openZonesForShots(String query) {
        scrim.layout();
        popup = Popup.ZONE;
        zoneQuery = query;
        showPopup();
    }

    private Table discardCard() {
        Table card = card("Confirm", "Discard your changes?");
        List<String> changed = new ArrayList<>();
        for (Section which : Section.values()) {
            if (dirty(which)) {
                changed.add(which.title);
            }
        }
        String names = changed.size() <= 1 ? String.join("", changed)
            : String.join(", ", changed.subList(0, changed.size() - 1)) + " and " + changed.get(changed.size() - 1);
        card.add(ui.paragraph("Your changes to " + names + " are lost. The server keeps its settings as they are.",
            Theme.TextStyle.BODY, Theme.INK)).padBottom(Theme.SPACE_4).row();
        Table buttons = new Table();
        buttons.add(ChromeScreen.onClick(ui.button("Keep editing", Theme.ButtonKind.SECONDARY), () -> {
            discarding = false;
            showSettings();
        })).padRight(10f);
        buttons.add(ChromeScreen.onClick(ui.button("Discard", Theme.ButtonKind.DESTRUCTIVE), () -> {
            discarding = false;
            close();
        }));
        card.add(buttons).right().fill(false, false).row();
        return card;
    }

    // ---------------------------------------------------------------------------------------------- popovers

    private void showPopup() {
        if (popupLayer == null) {
            return;
        }
        popupLayer.clearChildren();
        if (popup == Popup.NONE) {
            return;
        }
        layoutAll(scrim);
        Actor anchor = switch (popup) {
            case ZONE -> scrim.findActor("zone");
            case PACK -> scrim.findActor("pack");
            case CALENDAR -> scrim.findActor("date-" + calendarRow + "-" + (calendarEnd ? 1 : 0));
            default -> null;
        };
        if (anchor == null) {
            popup = Popup.NONE;
            return;
        }
        // A click anywhere else closes it.
        Table catcher = new Table();
        catcher.setFillParent(true);
        catcher.setTouchable(Touchable.enabled);
        catcher.addListener(SessionScreen.clicked(() -> {
            popup = Popup.NONE;
            popupLayer.clearChildren();
        }));
        popupLayer.addActor(catcher);
        Table card = switch (popup) {
            case ZONE -> zonePopup();
            case PACK -> packPopup();
            default -> calendarPopup();
        };
        float width = popup == Popup.CALENDAR ? 252f : anchor.getWidth();
        card.setWidth(width);
        card.setHeight(card.getPrefHeight());
        com.badlogic.gdx.math.Vector2 corner = anchor.localToActorCoordinates(scrim,
            new com.badlogic.gdx.math.Vector2(0f, 0f));
        float y = corner.y - card.getHeight() - 2f;
        if (y < 8f) {
            y = corner.y + anchor.getHeight() + 2f;
        }
        card.setPosition(corner.x, y);
        popupLayer.addActor(card);
    }

    /** Lays out everything now, so an anchor's place is known before the next frame draws. */
    private static void layoutAll(Actor actor) {
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.utils.Layout layout) {
            layout.validate();
        }
        if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
            for (Actor child : group.getChildren()) {
                layoutAll(child);
            }
        }
    }

    private Table popupCard() {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.top().left();
        card.defaults().growX().left();
        card.setTouchable(Touchable.enabled);
        card.addListener(new ClickListener());
        return card;
    }

    private Table zonePopup() {
        Table card = popupCard();
        TextField search = ui.searchField("Find a zone");
        search.setText(zoneQuery);
        search.setCursorPosition(zoneQuery.length());
        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                zoneQuery = search.getText();
                showPopup();
            }
        });
        search.setTextFieldListener((f, c) -> {
            if (c == '\r' || c == '\n') {
                List<String> matches = zoneMatches();
                if (!matches.isEmpty()) {
                    pickZone(matches.get(0));
                }
            }
        });
        card.add(searchStack(search)).height(32f).pad(Theme.SPACE_2).row();
        zoneList = new Table();
        zoneList.top().left();
        fillZoneList();
        card.add(zoneList).row();
        stage.setKeyboardFocus(search);
        return card;
    }

    private List<String> zoneMatches() {
        String query = zoneQuery.strip().toLowerCase(Locale.ROOT);
        return ZONES.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private void fillZoneList() {
        zoneList.clearChildren();
        List<String> matches = zoneMatches();
        for (int i = 0; i < Math.min(ZONE_ROWS, matches.size()); i++) {
            String id = matches.get(i);
            Table row = option(id, offset(id), i == 0, id.equals(zone));
            row.addListener(SessionScreen.clicked(() -> pickZone(id)));
            zoneList.add(row).growX().row();
            zoneList.add(rule()).height(Theme.BORDER_THIN).growX().row();
        }
        Table count = new Table();
        count.pad(8f, 12f, 8f, 12f);
        count.add(ui.label(matches.size() + " of " + ZONES.size() + " zones. Offsets as of today.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).expandX().left();
        zoneList.add(count).growX().row();
    }

    private void pickZone(String id) {
        zone = id;
        errors.remove("timezone");
        popup = Popup.NONE;
        showSettings();
    }

    private Table packPopup() {
        Table card = popupCard();
        for (ServerSettings.Pack option : saved.packs()) {
            Table row = new Table();
            row.pad(8f, 9f, 8f, 12f);
            boolean current = option.id().equals(pack);
            Table text = new Table();
            text.defaults().left();
            text.add(ui.label(option.id(), Theme.TextStyle.BODY, Theme.INK)).row();
            text.add(ui.paragraph(PACK_DESCRIPTIONS.getOrDefault(option.id(), ""), Theme.TextStyle.CAPTION,
                Theme.INK_MUTED)).growX();
            row.add(text).growX().left();
            if (current) {
                row.add(icon("check", Theme.NAVY)).size(14f).padLeft(Theme.SPACE_2);
            }
            Table entry = barred(row, current);
            entry.setTouchable(Touchable.enabled);
            entry.addListener(SessionScreen.clicked(() -> {
                if (!option.id().equals(pack)) {
                    pack = option.id();
                    goals.clear();
                    goals.addAll(option.defaultGoals());
                    goalQuery = "";
                }
                errors.remove("pack");
                errors.remove("goals");
                popup = Popup.NONE;
                showSettings();
            }));
            card.add(entry).row();
            card.add(rule()).height(Theme.BORDER_THIN).row();
        }
        return card;
    }

    /** A list entry: a light navy fill and a navy bar on the left while it's the active one. */
    private Table barred(Table inner, boolean on) {
        Table entry = new Table();
        if (on) {
            entry.setBackground(ui.solid(new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.07f)));
        }
        entry.add(new Image(ui.solid(on ? Theme.NAVY : new Color(0f, 0f, 0f, 0f)))).width(3f).growY();
        entry.add(inner).grow();
        return entry;
    }

    private Table option(String text, String note, boolean highlighted, boolean current) {
        Table row = new Table();
        row.pad(8f, 9f, 8f, 12f);
        row.add(ui.label(text, Theme.TextStyle.BODY, Theme.INK)).expandX().left();
        if (current) {
            row.add(icon("check", Theme.NAVY)).size(12f).padRight(Theme.SPACE_2);
        }
        row.add(ui.label(note, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).right();
        Table entry = barred(row, highlighted);
        entry.setTouchable(Touchable.enabled);
        return entry;
    }

    private Table calendarPopup() {
        Table card = popupCard();
        card.pad(10f);
        String[] range = breaks.get(calendarRow);
        LocalDate start = parse(range[0]);
        LocalDate end = parse(range[1]);
        LocalDate today = today();
        Table head = new Table();
        Table previous = new Table();
        previous.add(turned(icon("chevron", Theme.NAVY), 180f)).size(16f);
        previous.setTouchable(Touchable.enabled);
        previous.addListener(SessionScreen.clicked(() -> {
            calendarMonth = calendarMonth.minusMonths(1);
            showPopup();
        }));
        head.add(previous).size(24f);
        head.add(ui.label(calendarMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
            + " " + calendarMonth.getYear(), Theme.TextStyle.BODY_STRONG, Theme.INK)).expandX();
        Table next = iconButton("chevron", "Next month", () -> {
            calendarMonth = calendarMonth.plusMonths(1);
            showPopup();
        });
        head.add(next).size(24f);
        card.add(head).padBottom(6f).row();
        Table grid = new Table();
        for (DayOfWeek day : DayOfWeek.values()) {
            grid.add(ui.label(day.name().substring(0, 2), Theme.TextStyle.LABEL, Theme.INK_MUTED)).size(32f, 28f)
                .pad(1f);
        }
        grid.row();
        LocalDate first = calendarMonth.atDay(1);
        LocalDate cell = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        for (int week = 0; week < 6; week++) {
            for (int i = 0; i < 7; i++) {
                LocalDate date = cell;
                boolean inMonth = YearMonth.from(date).equals(calendarMonth);
                boolean pick = date.equals(start) || date.equals(end);
                boolean inRange = start != null && end != null && !date.isBefore(start) && !date.isAfter(end);
                Table day = new Table();
                Color fill = pick ? Theme.NAVY : inRange ? new Color(Theme.NAVY.r, Theme.NAVY.g, Theme.NAVY.b, 0.10f)
                    : new Color(0f, 0f, 0f, 0f);
                day.setBackground(date.equals(today) ? ui.rect(fill, Theme.GOLD, Theme.BORDER_EMPHASIS)
                    : ui.solid(fill));
                Color ink = pick ? Theme.ON_FILL : inMonth ? Theme.INK
                    : new Color(Theme.INK_MUTED.r, Theme.INK_MUTED.g, Theme.INK_MUTED.b, 0.45f);
                day.add(ui.label(Integer.toString(date.getDayOfMonth()), pick ? Theme.TextStyle.BODY_STRONG
                    : Theme.TextStyle.BODY_SM, ink));
                day.setTouchable(Touchable.enabled);
                day.addListener(SessionScreen.clicked(() -> {
                    range[calendarEnd ? 1 : 0] = date.toString();
                    errors.remove("breaks." + calendarRow);
                    popup = Popup.NONE;
                    showSettings();
                }));
                grid.add(day).size(32f, 28f).pad(1f);
                cell = cell.plusDays(1);
            }
            grid.row();
        }
        card.add(grid).row();
        return card;
    }

    // ---------------------------------------------------------------------------------------------- pieces

    private Table card(String eyebrow, String title) {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(Theme.SPACE_5 + Theme.BORDER_EMPHASIS);
        card.defaults().growX().left();
        card.add(ui.label(eyebrow.toUpperCase(Locale.ROOT), Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(2f).row();
        card.add(ui.paragraph(title, Theme.TextStyle.HEADING_SM, Theme.INK)).padBottom(Theme.SPACE_4).row();
        return card;
    }

    private Label fieldLabel(String text) {
        return ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED);
    }

    private Image rule() {
        return new Image(ui.solid(Theme.NAVY_BORDER));
    }

    private Image icon(String name, Color color) {
        Image image = new Image(ui.icon(name), Scaling.fit);
        image.setColor(color);
        return image;
    }

    /** Turns an icon about its centre, once its size is known (the chevron points right). */
    private static Actor turned(Image image, float degrees) {
        com.badlogic.gdx.scenes.scene2d.ui.Container<Image> holder =
            new com.badlogic.gdx.scenes.scene2d.ui.Container<>(image) {
                @Override
                public void layout() {
                    super.layout();
                    image.setOrigin(Align.center);
                }
            };
        holder.fill();
        image.setRotation(degrees);
        return holder;
    }

    private Table errorMark() {
        Table mark = new Table();
        mark.setBackground(ui.circle(Theme.DANGER, Theme.DANGER, 0f, 15));
        mark.add(ui.label("!", Theme.TextStyle.LABEL, Color.WHITE));
        return mark;
    }

    private Table errorLine(String text) {
        Table line = new Table();
        line.left();
        Table mark = new Table();
        mark.setBackground(ui.circle(Theme.DANGER, Theme.DANGER, 0f, 13));
        mark.add(ui.label("!", Theme.TextStyle.LABEL, Color.WHITE));
        line.add(mark).size(13f).padRight(5f).top().padTop(1f);
        line.add(ui.paragraph(text, Theme.TextStyle.CAPTION, Theme.DANGER)).growX().left();
        return line;
    }

    private Actor errorOrHint(String key, String hint) {
        if (errors.containsKey(key)) {
            return errorLine(errors.get(key));
        }
        return ui.paragraph(hint, Theme.TextStyle.CAPTION, Theme.INK_MUTED);
    }

    private Table lockLine(String text) {
        Table line = new Table();
        line.left();
        line.add(icon("lock", Theme.INK_MUTED)).size(12f).padRight(6f);
        line.add(ui.paragraph(text, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).growX().left();
        return line;
    }

    private Table lockedBox(String text) {
        Table box = new Table();
        box.setBackground(ui.rect(Theme.SKILL_TRACK, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        box.pad(0f, 10f, 0f, 10f);
        box.add(ui.label(text, Theme.TextStyle.BODY, Theme.INK)).expandX().left();
        box.add(icon("lock", Theme.INK_MUTED)).size(14f);
        return box;
    }

    private Table tag(String text) {
        Table tag = new Table();
        tag.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        tag.pad(2f, 6f, 2f, 6f);
        tag.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.INK_MUTED));
        return tag;
    }

    private TextField field(String text, String placeholder, boolean error, Consumer<String> typed) {
        TextField field = ui.textField(text, placeholder);
        ui.setFieldError(field, error);
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                typed.accept(field.getText());
                refreshChrome();
            }
        });
        return field;
    }

    private static void onChange(TextField field, Runnable action) {
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
    }

    private TextField passwordField(String text, String placeholder, boolean shown, boolean error) {
        TextField field = ui.textField(text, placeholder);
        ui.setFieldError(field, error);
        field.setStyle(roomOnTheRight(field.getStyle(), 30f));
        field.setPasswordCharacter('•');
        field.setPasswordMode(!shown);
        return field;
    }

    private static TextField.TextFieldStyle roomOnTheRight(TextField.TextFieldStyle style, float room) {
        TextField.TextFieldStyle padded = new TextField.TextFieldStyle(style);
        padded.background = widened(style.background, room);
        padded.focusedBackground = widened(style.focusedBackground, room);
        padded.disabledBackground = widened(style.disabledBackground, room);
        return padded;
    }

    private static Drawable widened(Drawable drawable, float room) {
        if (!(drawable instanceof NinePatchDrawable patch)) {
            return drawable;
        }
        NinePatchDrawable box = new NinePatchDrawable(patch);
        box.setRightWidth(patch.getRightWidth() + room);
        return box;
    }

    private Stack withEye(TextField field, boolean shown, Runnable toggle) {
        Table eye = new Table();
        eye.right();
        eye.add(iconButton(shown ? "eye_off" : "eye", shown ? "Hide the password" : "Show the password", toggle))
            .size(26f).padRight(4f);
        eye.setTouchable(Touchable.childrenOnly);
        return new Stack(field, eye);
    }

    private Stack dateField(String text, boolean error, Consumer<String> typed, Runnable calendar) {
        TextField field = field(text, "YYYY-MM-DD", error, typed);
        field.setStyle(roomOnTheRight(field.getStyle(), 30f));
        field.setMaxLength(10);
        Table button = new Table();
        button.right();
        button.add(iconButton("calendar", "Pick the date", calendar)).size(26f).padRight(4f);
        button.setTouchable(Touchable.childrenOnly);
        return new Stack(field, button);
    }

    private Stack searchStack(TextField search) {
        Table magnifier = new Table();
        magnifier.left();
        magnifier.add(icon("search", Theme.INK_MUTED)).size(14f).padLeft(Theme.SPACE_3);
        magnifier.setTouchable(Touchable.disabled);
        return new Stack(search, magnifier);
    }

    private Table selectBox(String text, String note, boolean error, Runnable open) {
        Table box = new Table();
        box.setBackground(ui.rect(Theme.SURFACE_CARD, error ? Theme.DANGER : Theme.NAVY,
            error ? Theme.BORDER_EMPHASIS : Theme.BORDER_THIN));
        box.pad(0f, 10f, 0f, 10f);
        box.add(ui.label(text, Theme.TextStyle.BODY, Theme.INK)).expandX().left();
        if (!note.isEmpty()) {
            box.add(ui.label(note, Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padRight(Theme.SPACE_2);
        }
        box.add(turned(icon("chevron", Theme.NAVY), -90f)).size(12f);
        box.setTouchable(Touchable.enabled);
        box.addListener(SessionScreen.clicked(open));
        return box;
    }

    private Table iconButton(String name, String label, Runnable action) {
        Table button = new Table();
        button.add(icon(name, Theme.NAVY)).size(16f);
        button.setTouchable(Touchable.enabled);
        button.setName(label);
        button.addListener(SessionScreen.clicked(action));
        return button;
    }

    private TextButton smallButton(String text, Runnable action) {
        TextButton button = ChromeScreen.onClick(ui.button(text, Theme.ButtonKind.SECONDARY), action);
        button.pad(5f, 10f, 5f, 10f);
        return button;
    }

    private Table iconTextButton(String iconName, String text, Runnable action) {
        Table button = new Table();
        button.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_THIN));
        button.pad(8f, 12f, 8f, 14f);
        button.add(icon(iconName, Theme.NAVY)).size(14f).padRight(6f);
        button.add(ui.label(text.toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL, Theme.NAVY));
        button.setTouchable(Touchable.enabled);
        button.addListener(SessionScreen.clicked(action));
        return button;
    }

    /** A segmented choice, as Settings' Sound; disabled, its segments go muted and don't click. */
    private Table segmented(String[] names, int selected, boolean enabled, java.util.function.IntConsumer pick) {
        Table segments = new Table();
        segments.setBackground(ui.rect(new Color(0f, 0f, 0f, 0f), Theme.NAVY_BORDER, Theme.BORDER_THIN));
        segments.pad(Theme.BORDER_THIN);
        for (int i = 0; i < names.length; i++) {
            int index = i;
            boolean on = i == selected;
            Table segment = new Table();
            Color fill = on ? (enabled ? Theme.NAVY : Theme.INK_MUTED) : Theme.SURFACE_CARD;
            segment.setBackground(ui.solid(fill));
            segment.add(ui.label(names[i].toUpperCase(Locale.ROOT), Theme.TextStyle.LABEL,
                on ? Theme.ON_FILL : enabled ? Theme.NAVY : Theme.INK_MUTED));
            if (enabled) {
                segment.setTouchable(Touchable.enabled);
                segment.addListener(SessionScreen.clicked(() -> pick.accept(index)));
            }
            segments.add(segment).expand().fill().uniformX();
        }
        if (!enabled) {
            segments.getColor().a = 0.7f;
        }
        return segments;
    }

    private void addWaitingChip(boolean show) {
        if (waitingChip != null) {
            waitingChip.remove();
            waitingChip = null;
        }
        if (!show) {
            return;
        }
        waitingChip = new Table();
        waitingChip.setBackground(ui.solid(Theme.NAVY));
        waitingChip.pad(8f, 14f, 8f, 14f);
        for (int i = 0; i < 3; i++) {
            waitingChip.add(new Image(ui.solid(Theme.ON_FILL))).size(6f).padRight(4f);
        }
        waitingChip.add(ui.label("WAITING FOR THE SERVER", Theme.TextStyle.LABEL, Theme.ON_FILL)).padLeft(6f);
        waitingChip.pack();
        waitingChip.setPosition((stage.getWidth() - waitingChip.getWidth()) / 2f,
            stage.getHeight() - 46f - waitingChip.getHeight());
        waitingChip.setTouchable(Touchable.disabled);
        scrim.addActor(waitingChip);
    }

    private void toast(String text, Theme.NoticeKind kind) {
        Table toast = new Table();
        toast.add(ui.notice(kind, text)).width(380f);
        toast.pack();
        toast.setPosition(stage.getWidth() - toast.getWidth() - 28f, 28f);
        toast.setTouchable(Touchable.disabled);
        toast.addAction(Actions.sequence(Actions.delay(3f), Actions.fadeOut(0.2f), Actions.removeActor()));
        scrim.addActor(toast);
    }
}
