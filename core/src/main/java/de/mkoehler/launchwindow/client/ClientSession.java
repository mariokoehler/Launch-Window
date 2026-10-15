package de.mkoehler.launchwindow.client;

import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.MissionDraft;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.ContentStrings;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What the client knows about the player's game while they're in it: who they are, their latest
 * state (with today's actions applied), the game info, and the game's content pack, loaded on the
 * client from the copy it bundles so screens can read names, caps and numbers (gap G7: the client
 * calls the same {@code core} code the server does, never its own copy of the rules).
 *
 * <p>The game's pack is identified by the game info's id and hash. If the client's bundled copy
 * hashes differently, the screens still use it, but {@link #packStatus()} says so: the download of
 * the game's own pack (gap G1, second half) isn't built yet.</p>
 *
 * @author Mario Koehler
 */
public final class ClientSession {

    /**
     * How the client's copy of the content pack compares with the game's.
     */
    public enum PackStatus {
        /** Identical to the game's frozen copy. */
        MATCH,
        /** The client has the pack, but a different version of it. */
        DIFFERENT
    }

    private final Credentials credentials;
    private final ContentCatalog catalog;
    private final PackStatus packStatus;
    private PlayerSnapshot state;
    private GameInfo gameInfo;
    private final Map<String, MissionDraft> drafts = new HashMap<>();
    private boolean planningFromList;
    private final Map<Long, TurnReport> reports = new HashMap<>();
    private long lastTurnWithoutReport;
    private List<RivalSummary> rivals;
    private List<FrontPage> frontPages;
    private List<ArchiveUnlock> archive;

    private ClientSession(Credentials credentials, ContentCatalog catalog, PackStatus packStatus, PlayerSnapshot state,
                          GameInfo gameInfo) {
        this.credentials = credentials;
        this.catalog = catalog;
        this.packStatus = packStatus;
        this.state = state;
        this.gameInfo = gameInfo;
    }

    /**
     * Starts a session from a successful login or claim, loading the game's content pack.
     *
     * @param credentials who and where
     * @param state       the player's state
     * @param gameInfo    the game info
     * @param classLoader the class loader holding the bundled packs under {@code content/}
     * @return the session
     * @throws IllegalStateException if the client doesn't bundle the game's pack at all
     */
    public static ClientSession start(Credentials credentials, PlayerSnapshot state, GameInfo gameInfo,
                                      ClassLoader classLoader) {
        Objects.requireNonNull(gameInfo, "gameInfo must not be null");
        String packId = gameInfo.response().contentPackId();
        String prefix = "content/" + packId;
        ContentCatalog catalog;
        String hash;
        try {
            catalog = ContentPackLoader.loadFromClasspath(prefix, classLoader);
            hash = ContentPackHash.ofClasspath(prefix, classLoader);
        } catch (RuntimeException e) {
            throw new IllegalStateException("This client doesn't have the game's content pack \"" + packId + "\".", e);
        }
        PackStatus status = hash.equals(gameInfo.response().contentPackHash()) ? PackStatus.MATCH : PackStatus.DIFFERENT;
        // The display text is the client's own bundled copy, never part of the frozen pack (ContentStrings).
        ContentNames.install(ContentStrings.loadFromClasspath(prefix, classLoader), catalog);
        return new ClientSession(credentials, catalog, status, state, gameInfo);
    }

    /**
     * Takes in a submit's or refresh's result: whatever state and game info it carries replace the
     * ones held.
     *
     * @param result the result
     */
    public void update(ServerResult result) {
        if (result.state() != null) {
            state = result.state();
        }
        if (result.gameInfo() != null) {
            gameInfo = result.gameInfo();
        }
    }

    /**
     * Returns who the player is and where they play.
     *
     * @return the credentials
     */
    public Credentials credentials() {
        return credentials;
    }

    /**
     * Returns the player's latest state.
     *
     * @return the state
     */
    public PlayerSnapshot state() {
        return state;
    }

    /**
     * Returns the latest game info.
     *
     * @return the game info
     */
    public GameInfo gameInfo() {
        return gameInfo;
    }

    /**
     * Returns the game's content pack.
     *
     * @return the catalog
     */
    public ContentCatalog catalog() {
        return catalog;
    }

    /**
     * Returns how the client's pack compares with the game's.
     *
     * @return the status
     */
    public PackStatus packStatus() {
        return packStatus;
    }

    /**
     * Returns the plan being drafted for a configuration, started on first use and brought up to
     * date with the current state ({@link MissionDraft#refit}) on every call.
     *
     * @param configuration the configuration
     * @return the draft
     */
    public MissionDraft draft(MissionConfiguration configuration) {
        MissionDraft draft = drafts.get(configuration.id());
        if (draft == null) {
            draft = MissionDraft.start(configuration, state, catalog);
            drafts.put(configuration.id(), draft);
        } else {
            draft.refit(state, catalog);
        }
        return draft;
    }

    /**
     * Returns whether the player came to the mission plan through Headquarters' "Plan a mission"
     * list, so the plan and assembly lead back there instead of to the configuration's page.
     *
     * @return whether planning started from the list
     */
    public boolean planningFromList() {
        return planningFromList;
    }

    /**
     * Records which way the player came to the mission plan.
     *
     * @param planningFromList whether from Headquarters' "Plan a mission" list
     */
    public void setPlanningFromList(boolean planningFromList) {
        this.planningFromList = planningFromList;
    }

    /**
     * Returns the game's open turn as this session last heard it: the turn its state belongs to and
     * its actions are meant for.
     *
     * @return the open turn
     */
    public long openTurn() {
        return gameInfo.response().gameTurn();
    }

    /**
     * Returns the latest turn that has resolved: the one before the game's open turn.
     *
     * @return the turn, 0 before the first resolution
     */
    public long latestResolvedTurn() {
        return Math.max(0, gameInfo.response().gameTurn() - 1);
    }

    /**
     * Returns a turn report fetched earlier in this session.
     *
     * @param turn the turn
     * @return the report, or {@code null} if it hasn't been fetched
     */
    public TurnReport report(long turn) {
        return reports.get(turn);
    }

    /**
     * Keeps a fetched turn report for the rest of the session; reports never change once written.
     *
     * @param report the report
     */
    public void keepReport(TurnReport report) {
        reports.put(report.gameTurn(), report);
    }

    /**
     * Returns the rival summaries fetched last in this session (gap G3).
     *
     * @return the summaries, or {@code null} if none were fetched yet
     */
    public List<RivalSummary> rivals() {
        return rivals;
    }

    /**
     * Keeps freshly fetched rival summaries.
     *
     * @param rivals every player's summary
     */
    public void keepRivals(List<RivalSummary> rivals) {
        this.rivals = List.copyOf(rivals);
    }

    /**
     * Returns the front pages fetched last in this session ({@code ui-spec.md} &sect;4.18).
     *
     * @return every page printed so far, oldest first, or {@code null} if none were fetched yet
     */
    public List<FrontPage> frontPages() {
        return frontPages;
    }

    /**
     * Keeps freshly fetched front pages.
     *
     * @param pages every page printed so far
     */
    public void keepFrontPages(List<FrontPage> pages) {
        this.frontPages = List.copyOf(pages);
    }

    /**
     * Returns the archive's goals reached in the game, fetched last in this session
     * ({@code design.md} &sect;2.12).
     *
     * @return the goals reached, or {@code null} if none were fetched yet
     */
    public List<ArchiveUnlock> archive() {
        return archive;
    }

    /**
     * Keeps a freshly fetched answer about the archive's goals.
     *
     * @param unlocks the goals reached
     */
    public void keepArchive(List<ArchiveUnlock> unlocks) {
        this.archive = List.copyOf(unlocks);
    }

    /**
     * Returns the game turn this player's agency joined in: its first resolved turn, from its own
     * season count (1 at the start, one more per resolved turn). Front pages printed before it
     * count as seen.
     *
     * @return the turn joined in
     */
    public long joinedTurn() {
        return openTurn() - Math.max(0, state().getSeason() - 1);
    }

    /**
     * Notes that the server has no report for a turn, so none for any earlier turn either: the
     * player joined after it resolved.
     *
     * @param turn the turn without a report
     */
    public void noReportAt(long turn) {
        lastTurnWithoutReport = Math.max(lastTurnWithoutReport, turn);
    }

    /**
     * Returns whether there may be a report for a turn: it has resolved, and it isn't known to
     * predate the player.
     *
     * @param turn the turn
     * @return whether to offer it
     */
    public boolean mayHaveReport(long turn) {
        return turn >= 1 && turn <= latestResolvedTurn() && turn > lastTurnWithoutReport;
    }

    /**
     * Forgets the draft for a configuration, after it launched.
     *
     * @param configurationId the configuration
     */
    public void clearDraft(String configurationId) {
        drafts.remove(configurationId);
    }
}
