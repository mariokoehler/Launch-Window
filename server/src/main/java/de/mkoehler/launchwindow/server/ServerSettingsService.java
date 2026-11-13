package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.AdminPasswords;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.SaveServerSettingsRequest;
import de.mkoehler.launchwindow.net.messages.ServerSettings;
import de.mkoehler.launchwindow.net.messages.ServerSettingsResponse;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.server.schedule.Blackout;
import de.mkoehler.launchwindow.server.schedule.BotPlayer;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameMetaStore;
import de.mkoehler.launchwindow.server.schedule.ScheduleProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * <p>The admin's server settings ({@code ui-spec.md} &sect;4.22, G21): reads {@code game-meta.json} for the dialog
 * and saves the admin's changes to it, after checking the admin password and every value the way startup does, so a
 * save never writes a file the server can't start from.</p>
 *
 * <p>A save is built from the file as it is now, never from what the client sent alone: only the fields that
 * aren't locked are taken from the request, so a hidden seed, the password and the bots already in the race carry
 * over. A hand edit of the file keeps working; a save over one is refused as stale.</p>
 *
 * @author Mario Koehler
 */
public final class ServerSettingsService {

    /** Wrong tries from one address before it's locked out (unconfirmed, design.md section 4). */
    static final int MAX_TRIES = 5;
    /** The window the tries count in, and how long a lockout lasts (unconfirmed). */
    static final long WINDOW_MILLIS = 10 * 60_000L;
    /** The longest agency or director name, as on Join game. */
    static final int MAX_NAME = 32;

    private static final System.Logger LOG = System.getLogger(ServerSettingsService.class.getName());
    private static final Pattern TIME = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");
    private static final Pattern BOT_ID = Pattern.compile("bot-[a-z0-9-]{1,32}");

    /** The reason the pack and seed are fixed, once an agency exists. */
    static final String LOCK_AGENCY = "Fixed once the first agency is founded.";
    /** The reason a field is fixed, once a turn resolved. */
    static final String LOCK_TURN = "Fixed once the first turn resolves.";

    /** The data root's folder of ended games, one zip each, kept through every new game. */
    public static final String OLD_GAMES = "old-games";

    private final Path dataRoot;
    private final Path metaFile;
    private final ObjectMapper mapper;
    private final ClassLoader classLoader;
    private final Clock clock;
    private final Map<String, List<String>> packGoals = new HashMap<>();
    private final Map<String, Deque<Long>> failures = new HashMap<>();
    private final Map<String, Long> lockedUntil = new HashMap<>();

    /**
     * Creates the service for a data root.
     *
     * @param dataRoot    the server's data root
     * @param mapper      the persistence mapper
     * @param classLoader where the bundled packs are
     * @param clock       the clock, for the lockout
     */
    public ServerSettingsService(Path dataRoot, ObjectMapper mapper, ClassLoader classLoader, Clock clock) {
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.metaFile = dataRoot.resolve("game-meta.json");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Makes sure {@code game-meta.json} has an admin password, making and saving one if not. Called at startup
     * only, never on the scheduler's every-tick read.
     *
     * @param file   the file
     * @param mapper the persistence mapper
     * @return the password
     * @throws IOException if the file can't be read or written
     */
    public static String ensureAdminPassword(Path file, ObjectMapper mapper) throws IOException {
        GameMeta meta = mapper.readValue(file.toFile(), GameMeta.class);
        if (meta.getAdminPassword() == null || meta.getAdminPassword().isBlank()) {
            meta.setAdminPassword(AdminPasswords.generate());
            GameMetaStore.save(file, meta, mapper);
        }
        return meta.getAdminPassword();
    }

    /**
     * Answers a request for the settings.
     *
     * @param password the admin password tried
     * @param host     the address the request came from
     * @param info     the game info now (the open turn, the next resolution)
     * @return the response
     */
    public ServerSettingsResponse read(String password, String host, GameInfoResponse info) {
        try {
            GameMeta meta = load();
            Optional<ServerSettingsResponse> refusal = checkPassword(meta, password, host);
            if (refusal.isPresent()) {
                return refusal.get();
            }
            return ServerSettingsResponse.ok(view(meta, version(), info));
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Couldn't read the server settings", e);
            return ServerSettingsResponse.failure(ServerSettingsResponse.Status.SERVER_ERROR, String.valueOf(e.getMessage()));
        }
    }

    /**
     * What a save came to.
     *
     * @param response    the answer for the client
     * @param saved       the settings as saved, or {@code null} if nothing was
     * @param packChanged whether the content pack changed (the server rebuilds its game)
     */
    public record SaveOutcome(ServerSettingsResponse response, GameMeta saved, boolean packChanged) {
    }

    /**
     * Saves the admin's changes, if the password is right, the file hasn't changed since they were read and every
     * value passes.
     *
     * @param request the request
     * @param host    the address it came from
     * @param info    the game info before the save
     * @param after   the game info after it, for the answer (the next resolution may move)
     * @return what happened
     */
    public SaveOutcome save(SaveServerSettingsRequest request, String host, GameInfoResponse info,
            java.util.function.Supplier<GameInfoResponse> after) {
        try {
            GameMeta current = load();
            Optional<ServerSettingsResponse> refusal = checkPassword(current, request.password(), host);
            if (refusal.isPresent()) {
                return new SaveOutcome(refusal.get(), null, false);
            }
            ServerSettings edit = request.settings();
            if (edit == null) {
                return new SaveOutcome(ServerSettingsResponse.failure(ServerSettingsResponse.Status.SERVER_ERROR,
                    "No settings were sent."), null, false);
            }
            if (!version().equals(edit.version())) {
                return new SaveOutcome(ServerSettingsResponse.failure(ServerSettingsResponse.Status.STALE_SETTINGS,
                    "The settings changed on the server since you opened them."), null, false);
            }
            Map<String, String> errors = new LinkedHashMap<>();
            GameMeta next = apply(current, edit, request.newPassword(), info.gameTurn(), errors);
            if (!errors.isEmpty()) {
                return new SaveOutcome(ServerSettingsResponse.invalid(errors), null, false);
            }
            GameMetaStore.save(metaFile, next, mapper);
            boolean packChanged = !next.getContentPack().equals(current.getContentPack());
            LOG.log(System.Logger.Level.INFO, "The admin saved the server settings from " + host
                + (packChanged ? "; the content pack is now " + next.getContentPack() : ""));
            return new SaveOutcome(ServerSettingsResponse.ok(view(next, version(), after.get())), next, packChanged);
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Couldn't save the server settings", e);
            return new SaveOutcome(ServerSettingsResponse.failure(ServerSettingsResponse.Status.SERVER_ERROR,
                String.valueOf(e.getMessage())), null, false);
        }
    }

    /** What survives a new game in the data root: the logins, the settings, and the old games' archives. */
    static final Set<String> KEPT_FOR_A_NEW_GAME = Set.of("accounts.json", "game-meta.json", OLD_GAMES);

    /**
     * What starting a new game came to.
     *
     * @param response the answer for the client
     * @param started  whether the old game was wiped (the server rebuilds its game and tells everyone)
     */
    public record NewGameOutcome(ServerSettingsResponse response, boolean started) {
    }

    /**
     * Ends the game and starts a fresh one ({@code ui-spec.md} &sect;4.22): the old game zipped into
     * {@value #OLD_GAMES}, everything in the data root deleted but the accounts and the settings, and a new random seed
     * saved. The caller rebuilds the game (the frozen pack, the bots' slots, the scheduler) and tells everyone online.
     *
     * @param request the request
     * @param host    the address it came from
     * @param after   the game info after it, for the answer
     * @return what happened
     */
    public NewGameOutcome startNewGame(de.mkoehler.launchwindow.net.messages.StartNewGameRequest request, String host,
            java.util.function.Supplier<GameInfoResponse> after) {
        try {
            GameMeta current = load();
            Optional<ServerSettingsResponse> refusal = checkPassword(current, request.password(), host);
            if (refusal.isPresent()) {
                return new NewGameOutcome(refusal.get(), false);
            }
            if (!version().equals(request.version())) {
                return new NewGameOutcome(ServerSettingsResponse.failure(ServerSettingsResponse.Status.STALE_SETTINGS,
                    "The settings changed on the server since you opened them."), false);
            }
            if (!de.mkoehler.launchwindow.net.messages.StartNewGameRequest.CONFIRMATION
                    .equals(request.confirmation().strip())) {
                return new NewGameOutcome(ServerSettingsResponse.invalid(Map.of("confirm", "Type "
                    + de.mkoehler.launchwindow.net.messages.StartNewGameRequest.CONFIRMATION + " to confirm.")), false);
            }
            Path archive = archiveOldGame();
            for (Path entry : listing(dataRoot)) {
                if (!KEPT_FOR_A_NEW_GAME.contains(entry.getFileName().toString())) {
                    deleteTree(entry);
                }
            }
            current.setGameSeed(new java.security.SecureRandom().nextLong());
            GameMetaStore.save(metaFile, current, mapper);
            LOG.log(System.Logger.Level.WARNING, "The admin started a new game from " + host + "; the old one is in "
                + dataRoot.relativize(archive));
            return new NewGameOutcome(ServerSettingsResponse.ok(view(current, version(), after.get())), true);
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Couldn't start a new game", e);
            // The archive comes first, so a wipe that stopped halfway loses nothing: trying again finishes it.
            return new NewGameOutcome(ServerSettingsResponse.failure(ServerSettingsResponse.Status.SERVER_ERROR,
                e.getMessage() + ". Try again; if it fails again, restart the server and try once more."), false);
        }
    }

    /**
     * Zips the game about to end into {@value #OLD_GAMES}, its frozen pack included and its turn backups left out,
     * so a new game started by mistake can be put back by hand (unzip it into a stopped server's data root).
     */
    private Path archiveOldGame() throws IOException {
        Path dir = dataRoot.resolve(OLD_GAMES);
        Files.createDirectories(dir);
        String stamp = java.time.LocalDateTime.now(clock).format(java.time.format.DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        Path zip = dir.resolve("game-" + stamp + ".zip");
        Path temp = Files.createTempFile(dir, "game-", ".zip.tmp");
        try {
            List<Path> files;
            try (Stream<Path> walk = Files.walk(dataRoot)) {
                files = walk.filter(Files::isRegularFile).filter(file -> {
                    String top = dataRoot.relativize(file).getName(0).toString();
                    return !top.equals(OLD_GAMES) && !top.equals("accounts.json")
                        && !top.equals(de.mkoehler.launchwindow.server.schedule.TurnBackups.DIR);
                }).sorted().toList();
            }
            try (java.io.OutputStream out = Files.newOutputStream(temp);
                    java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(out)) {
                for (Path file : files) {
                    StringBuilder name = new StringBuilder();
                    for (Path part : dataRoot.relativize(file)) {
                        name.append(name.isEmpty() ? "" : "/").append(part);
                    }
                    zipOut.putNextEntry(new java.util.zip.ZipEntry(name.toString()));
                    Files.copy(file, zipOut);
                    zipOut.closeEntry();
                }
            }
            Files.move(temp, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return zip;
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    private static List<Path> listing(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.toList();
        }
    }

    private static void deleteTree(Path path) throws IOException {
        if (Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            for (Path child : listing(path)) {
                deleteTree(child);
            }
        }
        Files.delete(path);
    }

    private GameMeta load() throws IOException {
        GameMeta meta = mapper.readValue(metaFile.toFile(), GameMeta.class);
        meta.validate();
        return meta;
    }

    /** The file's hash: a save carries it back, and is refused if the file changed meanwhile. */
    private String version() throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(metaFile)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<ServerSettingsResponse> checkPassword(GameMeta meta, String password, String host) {
        long now = clock.millis();
        Long until = lockedUntil.get(host);
        if (until != null && until > now) {
            int minutes = (int) Math.max(1, (until - now + 59_999) / 60_000);
            return Optional.of(new ServerSettingsResponse(ServerSettingsResponse.Status.ADMIN_LOCKED_OUT,
                "Too many wrong tries. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes."), minutes,
                null));
        }
        if (AdminPasswords.matches(meta.getAdminPassword(), password)) {
            failures.remove(host);
            return Optional.empty();
        }
        Deque<Long> tries = failures.computeIfAbsent(host, h -> new ArrayDeque<>());
        while (!tries.isEmpty() && tries.peekFirst() <= now - WINDOW_MILLIS) {
            tries.removeFirst();
        }
        tries.addLast(now);
        LOG.log(System.Logger.Level.WARNING, "A wrong admin password from " + host + " (" + tries.size() + " in "
            + WINDOW_MILLIS / 60_000 + " minutes)");
        if (tries.size() >= MAX_TRIES) {
            tries.clear();
            lockedUntil.put(host, now + WINDOW_MILLIS);
            int minutes = (int) (WINDOW_MILLIS / 60_000);
            return Optional.of(new ServerSettingsResponse(ServerSettingsResponse.Status.ADMIN_LOCKED_OUT,
                "Too many wrong tries. Try again in " + minutes + " minutes.", minutes, null));
        }
        return Optional.of(ServerSettingsResponse.failure(ServerSettingsResponse.Status.WRONG_ADMIN_PASSWORD,
            "That's not the admin password."));
    }

    /** Whether any agency exists: a player's or a bot's folder. */
    private boolean anyAgency() throws IOException {
        Path players = dataRoot.resolve("players");
        if (!Files.isDirectory(players)) {
            return false;
        }
        try (Stream<Path> folders = Files.list(players)) {
            return folders.anyMatch(Files::isDirectory);
        }
    }

    private String packLock(long openTurn) throws IOException {
        if (anyAgency()) {
            return LOCK_AGENCY;
        }
        return openTurn > 1 ? LOCK_TURN : null;
    }

    /** The game turn a bot's agency was founded on, or 0 if it has none. */
    private long botSince(String botId, long openTurn) throws IOException {
        Path folder = dataRoot.resolve("players").resolve(botId);
        if (!Files.isDirectory(folder)) {
            return 0;
        }
        var latest = new PlayerSnapshotStore(folder, mapper).latestTurnNumber();
        return latest.isPresent() ? Math.max(1, openTurn - latest.getAsLong()) : 0;
    }

    private ServerSettings view(GameMeta meta, String version, GameInfoResponse info) throws IOException {
        long openTurn = info.gameTurn();
        String packLock = packLock(openTurn);
        List<ServerSettings.Break> breaks = meta.getBlackouts().stream()
            .map(b -> new ServerSettings.Break(b.start().toString(), b.end().toString())).toList();
        List<ServerSettings.Bot> bots = new ArrayList<>();
        for (BotPlayer bot : meta.getBots()) {
            bots.add(new ServerSettings.Bot(bot.id(), bot.archetype().name(), nullToEmpty(bot.agencyName()),
                nullToEmpty(bot.directorName()), botSince(bot.id(), openTurn)));
        }
        List<ServerSettings.Pack> packs = new ArrayList<>();
        for (String pack : GameMeta.BUNDLED_PACKS) {
            List<String> goals = goals(pack);
            if (goals != null) {
                packs.add(new ServerSettings.Pack(pack, goals, GameMeta.defaultGoals(pack)));
            }
        }
        int agencies = PlayerSlots.everyPlayer(dataRoot, mapper).size();
        ScheduleProfile schedule = meta.getSchedule();
        return new ServerSettings(version, openTurn, agencies, info.nextResolutionMillis(),
            schedule.getActiveDays().stream().sorted().map(DayOfWeek::name).toList(),
            String.format(Locale.ROOT, "%02d:%02d", schedule.getResolutionTime().getHour(),
                schedule.getResolutionTime().getMinute()),
            schedule.getTimezone().getId(), breaks, meta.getContentPack(), meta.getDeclaredGoalIds(),
            packLock == null ? meta.getGameSeed() : null, meta.isDevMode(), meta.isDevArchiveOpen(), bots, packLock,
            openTurn > 1 ? LOCK_TURN : null, packs);
    }

    /** A bundled pack's goal ids in the pack's order, or {@code null} if it isn't bundled or doesn't load. */
    private List<String> goals(String packId) {
        if (!GameMeta.BUNDLED_PACKS.contains(packId)) {
            return null;
        }
        return packGoals.computeIfAbsent(packId, id -> {
            try {
                ContentCatalog catalog = ContentPackLoader.loadFromClasspath("content/" + id, classLoader);
                return catalog.goals().stream().map(Goal::id).toList();
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "The bundled pack " + id + " doesn't load", e);
                return null;
            }
        });
    }

    /**
     * Builds the settings to save: the file's, with the edit's unlocked fields applied. Every refused field goes
     * into {@code errors}, keyed as the dialog keys it.
     */
    private GameMeta apply(GameMeta current, ServerSettings edit, String newPassword, long openTurn,
            Map<String, String> errors) throws IOException {
        GameMeta next = mapper.readValue(mapper.writeValueAsBytes(current), GameMeta.class);

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String day : edit.activeDays()) {
            try {
                days.add(DayOfWeek.valueOf(day));
            } catch (IllegalArgumentException | NullPointerException e) {
                errors.put("days", "That's not a weekday: " + day + ".");
            }
        }
        if (days.isEmpty()) {
            errors.putIfAbsent("days", "Pick at least one day.");
        }
        LocalTime time = null;
        if (edit.resolutionTime() == null || !TIME.matcher(edit.resolutionTime()).matches()) {
            errors.put("time", "Not a time: use HH:MM, 00:00 to 23:59.");
        } else {
            time = LocalTime.parse(edit.resolutionTime());
        }
        ZoneId zone = null;
        try {
            zone = ZoneId.of(edit.timezone());
        } catch (RuntimeException e) {
            errors.put("timezone", "Not a time zone.");
        }
        if (!errors.containsKey("days") && time != null && zone != null) {
            next.setSchedule(new ScheduleProfile(days, time, zone));
        }

        List<Blackout> breaks = new ArrayList<>();
        for (int i = 0; i < edit.breaks().size(); i++) {
            ServerSettings.Break range = edit.breaks().get(i);
            try {
                LocalDate start = LocalDate.parse(range.start());
                LocalDate end = LocalDate.parse(range.end());
                if (start.isAfter(end)) {
                    errors.put("breaks." + i, "A break ends before it starts.");
                } else {
                    breaks.add(new Blackout(start, end));
                }
            } catch (DateTimeParseException | NullPointerException e) {
                errors.put("breaks." + i, "Not a date: use YYYY-MM-DD.");
            }
        }
        next.setBlackouts(breaks);

        String packLock = packLock(openTurn);
        if (packLock == null) {
            String pack = edit.contentPack();
            if (pack == null || goals(pack) == null) {
                errors.put("pack", "That pack isn't on this server.");
            } else {
                next.setContentPack(pack);
            }
            if (edit.gameSeed() != null) {
                next.setGameSeed(edit.gameSeed());
            }
        }
        if (openTurn <= 1) {
            List<String> goals = new ArrayList<>(new LinkedHashSet<>(edit.declaredGoalIds()));
            List<String> known = goals(next.getContentPack());
            if (goals.isEmpty()) {
                errors.put("goals", "At least one goal.");
            } else if (known != null && !known.containsAll(goals)) {
                errors.put("goals", "That goal isn't in this pack: " + goals.stream()
                    .filter(goal -> !known.contains(goal)).findFirst().orElse("") + ".");
            } else {
                next.setDeclaredGoalIds(goals);
            }
        }

        next.setDevMode(edit.devMode());
        next.setDevArchiveOpen(edit.devMode() && edit.devArchiveOpen());

        applyBots(current, next, edit, errors);

        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.strip().length() < AdminPasswords.MIN_LENGTH) {
                errors.put("password", "At least " + AdminPasswords.MIN_LENGTH + " characters.");
            } else {
                next.setAdminPassword(newPassword.strip());
            }
        }
        if (errors.isEmpty()) {
            next.validate();
        }
        return next;
    }

    /**
     * Keeps every bot whose agency exists as the file has it, and takes the edit's other bots, checking their ids
     * and names (a name taken would leave the bot without an agency at startup).
     */
    private void applyBots(GameMeta current, GameMeta next, ServerSettings edit, Map<String, String> errors)
            throws IOException {
        List<BotPlayer> bots = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (BotPlayer bot : current.getBots()) {
            if (Files.isDirectory(dataRoot.resolve("players").resolve(bot.id()))) {
                bots.add(bot);
                ids.add(bot.id());
            }
        }
        Set<String> agencies = new HashSet<>();
        Set<String> directors = new HashSet<>();
        for (PlayerSnapshot player : PlayerSlots.everyPlayer(dataRoot, mapper)) {
            agencies.add(player.getAgencyName().toLowerCase(Locale.ROOT));
            directors.add(player.getDirectorName().toLowerCase(Locale.ROOT));
        }
        for (int i = 0; i < edit.bots().size(); i++) {
            ServerSettings.Bot bot = edit.bots().get(i);
            if (bot.id() != null && ids.contains(bot.id())) {
                continue;
            }
            String key = "bots." + i;
            if (bot.id() == null || !BOT_ID.matcher(bot.id()).matches()
                    || Files.exists(dataRoot.resolve("players").resolve(bot.id())) || !ids.add(bot.id())) {
                errors.put(key + ".id", "That id is taken or isn't bot- and lower-case letters or digits.");
                continue;
            }
            Archetype archetype;
            try {
                archetype = Archetype.valueOf(bot.archetype());
            } catch (IllegalArgumentException | NullPointerException e) {
                errors.put(key + ".side", "Pick NASA or Soviet.");
                continue;
            }
            String agency = nullToEmpty(bot.agencyName()).strip();
            String director = nullToEmpty(bot.directorName()).strip();
            if (agency.length() > MAX_NAME) {
                errors.put(key + ".agency", "At most " + MAX_NAME + " characters.");
            } else if (!agency.isEmpty() && !agencies.add(agency.toLowerCase(Locale.ROOT))) {
                errors.put(key + ".agency", "Already taken by another agency.");
            }
            if (director.length() > MAX_NAME) {
                errors.put(key + ".director", "At most " + MAX_NAME + " characters.");
            } else if (!director.isEmpty() && !directors.add(director.toLowerCase(Locale.ROOT))) {
                errors.put(key + ".director", "Already taken by another director.");
            }
            bots.add(new BotPlayer(bot.id(), archetype, agency.isEmpty() ? null : agency,
                director.isEmpty() ? null : director));
        }
        next.setBots(bots);
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
