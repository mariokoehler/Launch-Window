package de.mkoehler.launchwindow.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ClaimResult;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.client.connect.ServerGateway;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.client.screen.ConnectScreen;
import de.mkoehler.launchwindow.client.settings.SettingsStore;
import de.mkoehler.launchwindow.client.screen.JoinScreen;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.screen.BodyProgramsScreen;
import de.mkoehler.launchwindow.client.screen.HeadquartersScreen;
import de.mkoehler.launchwindow.client.screen.OpenProgramsScreen;
import de.mkoehler.launchwindow.client.screen.ProgramDetailsScreen;
import de.mkoehler.launchwindow.client.screen.SelectBodyScreen;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.client.screen.ExplainerScreen;
import de.mkoehler.launchwindow.client.screen.HireScreen;
import de.mkoehler.launchwindow.client.screen.SpaceComplexScreen;
import de.mkoehler.launchwindow.client.screen.RoadsToMoonScreen;
import de.mkoehler.launchwindow.client.view.Explainers;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.client.screen.RndBoardScreen;
import de.mkoehler.launchwindow.client.screen.OpenRocketScreen;
import de.mkoehler.launchwindow.client.screen.RocketProgramsScreen;
import de.mkoehler.launchwindow.client.screen.VabScreen;
import de.mkoehler.launchwindow.client.screen.MuseumScreen;
import de.mkoehler.launchwindow.client.screen.ArchivePageScreen;
import de.mkoehler.launchwindow.client.screen.StatementDialog;
import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.rules.ReactionPrinter;
import de.mkoehler.launchwindow.client.screen.FrontPageScreen;
import de.mkoehler.launchwindow.client.screen.AstronautComplexScreen;
import de.mkoehler.launchwindow.client.screen.PublicAffairsScreen;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.NewsEntry;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.client.screen.ReplayMonitor;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.client.view.ReplayConsole;
import de.mkoehler.launchwindow.client.view.ReplayClips;
import de.mkoehler.launchwindow.client.screen.ReplayScreen;
import de.mkoehler.launchwindow.client.screen.TurnReportScreen;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.PrDutyPerformed;
import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.model.HiringPoolBonus;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.TurnReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import de.mkoehler.launchwindow.client.screen.MissionControlScreen;
import de.mkoehler.launchwindow.client.screen.MissionsScreen;
import de.mkoehler.launchwindow.client.screen.RestaffScreen;
import de.mkoehler.launchwindow.client.view.Department;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.rules.LaunchRequest;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelNames;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionStaffing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import de.mkoehler.launchwindow.client.screen.AssembleMissionScreen;
import de.mkoehler.launchwindow.client.screen.MissionConfigsScreen;
import de.mkoehler.launchwindow.client.screen.MissionPlanScreen;
import de.mkoehler.launchwindow.client.screen.MissionStepsScreen;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.client.screen.SetCentreScreen;
import de.mkoehler.launchwindow.client.view.PayloadRnd;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Renders every state of the client's screens to PNG files on a hidden window, with no server
 * ({@code design.md} &sect;3.11, the same mechanism as Robot-Rampage's own tool): each screen
 * is built against a gateway that never answers, put into the state a mockup shows, drawn into
 * a frame buffer and written out. Never shipped; a developer's way to compare a screen with
 * its mockup, including states that only appear after a server reply.
 *
 * <p>Run from the {@code assets/} folder (see {@code CLAUDE.md} for the classpath recipe),
 * with the output folder as the first argument. An optional second argument such as
 * {@code 1632x918} renders at that window size instead of the design's 1920 by 1080, to see how
 * thin borders and small text hold up when scaled down.</p>
 *
 * @author Mario Koehler
 */
public final class ScreenSnapshot {

    private static int width = Theme.VIEW_WIDTH;
    private static int height = Theme.VIEW_HEIGHT;
    private static final Credentials CREDENTIALS =
        new Credentials(new ServerAddress("localhost", 45825), "mario", "hunter2");

    /**
     * Not instantiable; this class only holds the tool's entry point.
     */
    private ScreenSnapshot() {
    }

    /**
     * A gateway that never answers: screens can be built, but no call ever comes back.
     */
    private static final class SilentGateway implements ServerGateway {
        /** What {@link #isWaiting()} says, for the waiting indicator's shot. */
        boolean waiting;

        @Override
        public boolean isWaiting() {
            return waiting;
        }

        @Override
        public void logIn(Credentials credentials, Consumer<LoginResult> done) {
        }

        @Override
        public void claimSlot(Credentials credentials, Archetype archetype, String agencyName, String directorName,
                              Consumer<ClaimResult> done) {
        }

        @Override
        public void submit(Credentials credentials, PlayerAction action, long gameTurn,
                Consumer<ServerResult> done) {
        }

        @Override
        public void refresh(Credentials credentials, Consumer<ServerResult> done) {
        }

        @Override
        public void fetchTurnReport(Credentials credentials, long turn,
                                    Consumer<de.mkoehler.launchwindow.client.connect.TurnReportResult> done) {
        }

        @Override
        public void resolveTurnNow(Credentials credentials, Consumer<ServerResult> done) {
        }

        @Override
        public void fetchRivals(Credentials credentials,
                                Consumer<de.mkoehler.launchwindow.client.connect.RivalsResult> done) {
        }

        @Override
        public void previewPublication(Credentials credentials, String goalId,
                Consumer<de.mkoehler.launchwindow.client.connect.PublishPreviewResult> done) {
            // A fixed answer, for the confirmation dialog's shot: helps two of three rivals.
            done.accept(new de.mkoehler.launchwindow.client.connect.PublishPreviewResult(true, "", 287, 2, 3));
        }

        @Override
        public void logOut() {
        }

        @Override
        public void dispose() {
        }
    }

    /**
     * Writes every snapshot.
     *
     * @param args the output folder (the working directory if none), then optionally a size such as {@code 1632x918}
     */
    public static void main(String[] args) {
        File folder = new File(args.length > 0 ? args[0] : ".");
        if (args.length > 1) {
            String[] size = args[1].split("x");
            width = Integer.parseInt(size[0]);
            height = Integer.parseInt(size[1]);
        }
        folder.mkdirs();
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setInitialVisible(false);
        configuration.setWindowedMode(width, height);
        // Silent, and the sound setting never this machine's: the Join screen would speak otherwise.
        configuration.disableAudio(true);
        LaunchWindowGame.useSoundInMemory(true);
        // The shots render at the game's default text size, Large, unless -Dui.textScale says otherwise;
        // the Settings shot shows Large chosen when the shots are taken at its size.
        float textScale = Float.parseFloat(System.getProperty("ui.textScale", String.valueOf(LaunchWindowGame.TextSize.LARGE.scale())));
        LaunchWindowGame.useTextSizeInMemory(textScale == LaunchWindowGame.TextSize.LARGE.scale()
            ? LaunchWindowGame.TextSize.LARGE : LaunchWindowGame.TextSize.STANDARD);
        new Lwjgl3Application(new LaunchWindowGame() {
            @Override
            protected ServerGateway createGateway() {
                return new SilentGateway();
            }

            @Override
            public com.badlogic.gdx.video.VideoPlayer createVideoPlayer() {
                // No video in the shots: the replay's monitor shows its still, the same every run;
                // the console's shots use a stand-in that shows one frame of the launch clip.
                return fakeVideo ? fakeVideoPlayer() : null;
            }

            @Override
            protected SettingsStore createSettingsStore() {
                // Never the player's real file: the shots always start from the defaults.
                return new SettingsStore(folder.toPath().resolve("no-such-folder").resolve("client-settings.json"));
            }

            @Override
            public void create() {
                super.create();
                // -Dui.textScale=1 draws the text at the design's size (Standard in Settings); any factor
                // can be tried. The shots' text-fit report then shows what it breaks (TextFit).
                ui().fonts().setTextScale(textScale);
                renderAll(this, folder);
                writeTextFit(folder);
                Gdx.app.exit();
            }
        }, configuration);
    }

    private static void renderAll(LaunchWindowGame game, File folder) {
        // Explainers' read marks in memory, never this machine's: every explainer starts unread.
        game.useExplainerReadMarksInMemory(new java.util.HashSet<>());
        // The advisor's marks too: every run starts with help unopened and the advisor on duty.
        // The idle tip done: only its own shots show it (groundingShots).
        game.useAdvisorMarksInMemory(new java.util.HashSet<>(java.util.Set.of("idleTipDone")));
        // -Dshots=contracts renders only one group, for a quick look at the screens being worked on.
        if ("contracts".equals(System.getProperty("shots"))) {
            contractShots(folder, game);
            return;
        }
        if ("handson".equals(System.getProperty("shots"))) {
            handsOnShots(folder, game);
            return;
        }
        if ("decisions".equals(System.getProperty("shots"))) {
            decisionPoolShots(folder, game);
            return;
        }
        if ("frontpage".equals(System.getProperty("shots"))) {
            frontPageShots(folder, game);
            return;
        }
        if ("reactions".equals(System.getProperty("shots"))) {
            reactionShots(folder, game);
            return;
        }
        if ("theories".equals(System.getProperty("shots"))) {
            theoryShots(folder, game);
            return;
        }
        if ("memorial".equals(System.getProperty("shots"))) {
            memorialShots(folder, game);
            return;
        }
        if ("grounding".equals(System.getProperty("shots"))) {
            groundingShots(folder, game);
            return;
        }
        if ("rival".equals(System.getProperty("shots"))) {
            rivalFirstsShots(folder, game);
            return;
        }
        if ("raise".equals(System.getProperty("shots"))) {
            raiseShots(folder, game);
            return;
        }
        if ("join".equals(System.getProperty("shots"))) {
            joinNameShots(folder, game);
            return;
        }
        if ("explainers".equals(System.getProperty("shots"))) {
            String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
            startSession(game, new PlayerSnapshot("Baltic Star Agency", Archetype.NASA, 24_310, 14_280, 3_400, 52),
                GameInfoResponse.success("standard", hash, List.of(), 52, true, 6 * 3_600_000L, 0L, null, List.of(),
                    List.of(), 0));
            explainerShots(folder, game);
            return;
        }
        if ("archive".equals(System.getProperty("shots"))) {
            archiveShots(folder, game);
            return;
        }
        if ("findings".equals(System.getProperty("shots"))) {
            findingShots(folder, game);
            return;
        }
        if ("console".equals(System.getProperty("shots"))) {
            consoleShots(folder, game);
            return;
        }
        if ("intel".equals(System.getProperty("shots"))) {
            intelligenceShots(folder, game);
            return;
        }
        if ("poaching".equals(System.getProperty("shots"))) {
            poachingShots(folder, game);
            return;
        }
        if ("replay".equals(System.getProperty("shots"))) {
            String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
            reportShots(folder, game, GameInfoResponse.success("early-era", hash, List.of(), 1, true,
                6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0));
            return;
        }
        if ("roads".equals(System.getProperty("shots"))) {
            roadsShots(folder, game);
            return;
        }
        if ("progression".equals(System.getProperty("shots"))) {
            progressionShots(folder, game);
            return;
        }
        if ("connect".equals(System.getProperty("shots"))) {
            write(folder, "connect-no-account.png", filledConnect(game), screen -> screen.applyLoginResult(
                CREDENTIALS, LoginResult.failure(LoginResult.Kind.NO_ACCOUNT, "")));
            return;
        }
        write(folder, "connect-default.png", new ConnectScreen(game), null);
        write(folder, "connect-connecting.png", filledConnect(game), screen -> screen.setBusy(true));
        write(folder, "connect-wrong-password.png", filledConnect(game), screen -> screen.applyLoginResult(
            CREDENTIALS, LoginResult.failure(LoginResult.Kind.REJECTED, "Incorrect password for 'mario'.")));
        write(folder, "connect-invalid-login.png", filledConnect(game), screen -> {
            screen.loginField().input().setText("mario köhler");
            screen.submit();
        });
        write(folder, "connect-version-mismatch.png", filledConnect(game), screen -> screen.applyLoginResult(
            CREDENTIALS, new LoginResult(LoginResult.Kind.VERSION_MISMATCH, "", "0.4.0", "0.5.0", null, null)));
        write(folder, "connect-unreachable.png", filledConnect(game), screen -> screen.applyLoginResult(
            CREDENTIALS, LoginResult.failure(LoginResult.Kind.UNREACHABLE, "Connection refused")));
        write(folder, "connect-no-account.png", filledConnect(game), screen -> screen.applyLoginResult(
            CREDENTIALS, LoginResult.failure(LoginResult.Kind.NO_ACCOUNT, "")));

        write(folder, "join-default.png", new JoinScreen(game, CREDENTIALS), null);
        write(folder, "join-selected.png", new JoinScreen(game, CREDENTIALS), screen -> {
            screen.selectArchetype(Archetype.SOVIET);
            screen.agencyNameField().input().setText("Baltic Star Agency");
            screen.directorNameField().input().setText("A. Reyes");
        });
        write(folder, "join-empty-fields.png", new JoinScreen(game, CREDENTIALS), JoinScreen::submit);
        write(folder, "join-agency-taken.png", filledJoin(game), screen -> screen.applyClaimResult(
            ClaimResult.refused(ClaimSlotResponse.failure(ClaimSlotResponse.Reason.AGENCY_NAME_TAKEN, "Taken"))));
        write(folder, "join-director-taken.png", filledJoin(game), screen -> screen.applyClaimResult(
            ClaimResult.refused(ClaimSlotResponse.failure(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN, "Taken"))));
        write(folder, "join-joining.png", filledJoin(game), screen -> screen.setBusy(true));
        write(folder, "join-history-pinned.png", new JoinScreen(game, CREDENTIALS),
            screen -> screen.directorNameField().historyNote().setPinned(true));
        joinNameShots(folder, game);

        PlayerSnapshot state = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        state.setDirectorName("A. Reyes");
        GameInfoResponse info = GameInfoResponse.success("early-era", "hash", List.of(), 1, false,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        game.setSession(ClientSession.start(CREDENTIALS, state, new GameInfo(info, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
        write(folder, "hub-pack-differs.png", new SpaceComplexScreen(game), null);
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse spring = GameInfoResponse.success("early-era", hash, List.of(), 2, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        game.setSession(ClientSession.start(CREDENTIALS, state, new GameInfo(spring, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
        write(folder, "hub-soviet-spring.png", new SpaceComplexScreen(game), null);
        write(folder, "hub-soviet-hover-hq.png", new SpaceComplexScreen(game),
            screen -> screen.hover(Hub.Place.HEADQUARTERS));
        PlayerSnapshot nasa = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 27_420, 250, 3_000, 3);
        nasa.setDirectorName("A. Reyes");
        nasa.getBuildingUpgradeSeasonsRemaining().put(Building.VEHICLE_ASSEMBLY, 1);
        GameInfoResponse summer = GameInfoResponse.success("early-era", hash, List.of(), 3, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        game.setSession(ClientSession.start(CREDENTIALS, nasa, new GameInfo(summer, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
        write(folder, "hub-nasa-summer-upgrading.png", new SpaceComplexScreen(game),
            screen -> screen.hover(Hub.Place.SET_CENTRE));

        game.setSession(ClientSession.start(CREDENTIALS, setCentreState(), new GameInfo(info, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
        write(folder, "set-overview.png", new SetCentreScreen(game), null);
        write(folder, "set-training-permanent.png", new SetCentreScreen(game), screen -> screen.openTraining(
            game.session().state().getSetSpecialists().get(0), TrainingKind.PERMANENT, SetSkill.SPACE_PROBES));
        write(folder, "set-training-advanced.png", new SetCentreScreen(game), screen -> screen.openTraining(
            game.session().state().getSetSpecialists().get(3), TrainingKind.ADVANCED, SetSkill.ROCKETS));
        write(folder, "set-confirm-dismiss.png", new SetCentreScreen(game), screen -> screen.confirmDismiss(
            game.session().state().getSetSpecialists().get(2)));
        write(folder, "set-manage-department.png", new SetCentreScreen(game),
            screen -> screen.openManageDepartment(-10));
        game.session().state().getSalaryAdjustedGroups().add("SET");
        write(folder, "set-manage-already-adjusted.png", new SetCentreScreen(game),
            screen -> screen.openManageDepartment(-10));
        game.session().state().getSalaryAdjustedGroups().clear();
        write(folder, "set-toast.png", new SetCentreScreen(game), screen -> screen.toast(
            "Started training \u2014 Elena Markova (Permanent, Space probes).", Theme.NoticeKind.SUCCESS));
        write(folder, "payload-rnd.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), null);
        write(folder, "payload-rnd-assign.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), screen -> screen.openAssign(
            PayloadRnd.cards(game.session().state(), game.session().catalog()).get(2)));
        write(folder, "hire.png", new HireScreen(game), screen -> {
            List<SetSpecialist> pool = game.session().state().getSetSpecialistCandidates();
            screen.toggle(pool.get(1).getId());
            screen.toggle(pool.get(4).getId());
            screen.toggleBonus(pool.get(1).getId());
        });
        write(folder, "hire-none-selected.png", new HireScreen(game), null);

        PlayerSnapshot fresh = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        fresh.setDirectorName("A. Reyes");
        game.setSession(ClientSession.start(CREDENTIALS, fresh, new GameInfo(info, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
        game.setSession(ClientSession.start(CREDENTIALS, headquartersState(), new GameInfo(info,
            System.currentTimeMillis()), ScreenSnapshot.class.getClassLoader()));
        write(folder, "hq-overview.png", new HeadquartersScreen(game), null);
        write(folder, "hq-upgrade.png", new HeadquartersScreen(game), screen -> screen.confirmUpgrade(
            Building.HEADQUARTERS, "Headquarters", "open-program cap"));
        write(folder, "hq-open-programs.png", new OpenProgramsScreen(game), null);
        write(folder, "hq-select-body.png", new SelectBodyScreen(game), null);
        write(folder, "hq-body-earth.png", new BodyProgramsScreen(game, CelestialBody.EARTH), null);
        write(folder, "hq-body-earth-capsules.png", new BodyProgramsScreen(game, CelestialBody.EARTH),
            screen -> screen.select("one-crew-capsules"));
        write(folder, "hq-details-vostok.png", new ProgramDetailsScreen(game, "vostok", CelestialBody.EARTH), null);
        write(folder, "hq-details-vostok-open.png", new ProgramDetailsScreen(game, "vostok", CelestialBody.EARTH),
            screen -> screen.tryOpen(game.session().catalog().program("vostok").orElseThrow(),
                Programs.price(game.session().catalog().program("vostok").orElseThrow(), game.session().state(),
                    game.session().catalog())));
        write(folder, "hq-details-sputnik-1.png", new ProgramDetailsScreen(game, "sputnik-1", null), null);
        // Its satellite is new, so the upkeep is itemized: the program's and the satellite's.
        write(folder, "hq-details-sputnik-2-open.png", new ProgramDetailsScreen(game, "sputnik-2", CelestialBody.EARTH),
            screen -> screen.tryOpen(game.session().catalog().program("sputnik-2").orElseThrow(),
                Programs.price(game.session().catalog().program("sputnik-2").orElseThrow(), game.session().state(),
                    game.session().catalog())));
        // The longest history note in the pack (the Sputnik 1 satellite's), pinned open.
        write(folder, "hq-details-sputnik-1-note.png", new ProgramDetailsScreen(game, "sputnik-1", null),
            screen -> pinLastHistoryNote(screen.stage().getRoot()));
        write(folder, "hq-details-sputnik-2.png", new ProgramDetailsScreen(game, "sputnik-2", CelestialBody.EARTH), null);
        write(folder, "hq-details-offer-rnd.png", new ProgramDetailsScreen(game, "sputnik-2", CelestialBody.EARTH),
            screen -> {
                // As just after opening: its satellite active and unstaffed, only while the dialog is built.
                var satellite = game.session().state().getMissionComponents().get("sputnik-2-satellite");
                satellite.setActive(true);
                screen.offerResearch(game.session().catalog().program("sputnik-2").orElseThrow(), "$316.0");
                satellite.setActive(false);
            });

        game.setSession(ClientSession.start(CREDENTIALS, fresh, new GameInfo(info, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
        write(folder, "hq-open-programs-empty.png", new OpenProgramsScreen(game), null);
        write(folder, "set-empty.png", new SetCentreScreen(game), null);
        write(folder, "payload-rnd-empty.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), null);

        game.setSession(ClientSession.start(CREDENTIALS, vabState(false), new GameInfo(info,
            System.currentTimeMillis()), ScreenSnapshot.class.getClassLoader()));
        write(folder, "vab-overview.png", new VabScreen(game), null);
        write(folder, "vab-upgrade.png", new VabScreen(game), screen -> screen.confirmUpgrade(
            Building.VEHICLE_ASSEMBLY, "the VAB", "rocket-program cap", "the Medium class"));
        write(folder, "vab-open.png", new OpenRocketScreen(game), null);
        write(folder, "vab-open-confirm.png", new OpenRocketScreen(game), screen -> screen.tryOpen(
            game.session().catalog().missionComponent("r7-vostok").orElseThrow()));
        write(folder, "vab-rocket-rnd.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET), null);
        write(folder, "vab-open-tooltip-leo.png", new OpenRocketScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "LEO"));
        write(folder, "vab-open-tooltip-reliability.png", new OpenRocketScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "Reliability on opening"));
        write(folder, "vab-open-offer-rnd.png", new OpenRocketScreen(game), screen -> screen.offerResearch(
            game.session().catalog().missionComponent("r7-sputnik").orElseThrow(), "R-7 Sputnik"));
        write(folder, "vab-rocket-rnd-focus.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET,
            "r7-sputnik"), null);
        game.setSession(ClientSession.start(CREDENTIALS, vabState(true), new GameInfo(info,
            System.currentTimeMillis()), ScreenSnapshot.class.getClassLoader()));
        write(folder, "vab-manage.png", new RocketProgramsScreen(game), null);
        write(folder, "vab-manage-close.png", new RocketProgramsScreen(game), screen -> screen.confirmClose(
            game.session().catalog().missionComponent("r7-vostok").orElseThrow()));
        write(folder, "vab-open-empty.png", new OpenRocketScreen(game), null);
        write(folder, "vab-overview-upgrading.png", new VabScreen(game), null);

        planningShots(folder, game, info);
        missionControlShots(folder, game, info);
        reportShots(folder, game, info);
        museumShots(folder, game, info);
        peopleShots(folder, game, info);

        // Plan a mission (play test, 2026-10-07): straight from Headquarters, and the plan it opens.
        startSession(game, planningState(11, 25_000), info);
        write(folder, "hq-overview-plan.png", new HeadquartersScreen(game), null);
        write(folder, "hq-plan-mission.png", new de.mkoehler.launchwindow.client.screen.PlanMissionScreen(game), null);
        game.session().setPlanningFromList(true);
        write(folder, "hq-plan-mission-plan.png", new MissionPlanScreen(game, "vostok-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);
        game.session().setPlanningFromList(false);
        PlayerSnapshot none = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        none.setDirectorName("A. Reyes");
        startSession(game, none, info);
        write(folder, "hq-plan-mission-empty.png", new de.mkoehler.launchwindow.client.screen.PlanMissionScreen(game),
            null);

        intelligenceShots(folder, game);
        careerShots(folder, game, info);
        poachingShots(folder, game);
        helpShots(folder, game);
        advisorShots(folder, game);
        standardPackShots(folder, game);
        outlookShots(folder, game);
        decisionShots(folder, game);
        contractShots(folder, game);
        handsOnShots(folder, game);
        frontPageShots(folder, game);
        groundingShots(folder, game);
        rivalFirstsShots(folder, game);
        raiseShots(folder, game);
        consoleShots(folder, game);
        archiveShots(folder, game);
        findingShots(folder, game);
        reactionShots(folder, game);
        theoryShots(folder, game);
        memorialShots(folder, game);
        progressionShots(folder, game);
        roadsShots(folder, game);
    }

    /**
     * Findings and publishing them (design.md 2.13, 2026-10-21): the turn report's Findings card with a
     * finding earned, the agency's publication and a rival's; the confirmation dialog; the Museum's list
     * of known findings; a finding's archive page with its Publish button; and the mission plan's odds
     * with the steps a known finding makes safer marked.
     */
    private static void findingShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 52, true,
            6 * 3_600_000L + 12 * 60_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot me = new PlayerSnapshot("Baltic Star Agency", Archetype.NASA, 24_310, 14_280, 3_400, 52);
        me.setDirectorName("A. Reyes");
        me.getAchievedGoals().put("mars-mapping", 51);
        me.getAchievedGoals().put("lunar-mapping", 44);
        me.getAchievedGoals().put("magnetic-field-study", 6);
        me.getAchievedGoals().put("micrometeoroid-study", 20);
        me.getPendingPublications().add("micrometeoroid-study");
        me.getPublishedFindings().add("lunar-mapping");
        me.getPublicFindings().put("lunar-mapping", 51);
        me.getPublicFindings().put("venus-atmosphere-study", 51);
        startSession(game, me, info);
        game.useArchiveReadMarksInMemory(new java.util.HashSet<>());
        List<de.mkoehler.launchwindow.net.messages.ArchiveUnlock> unlocks = List.of(
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("lunar-mapping", 44, true),
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("mars-mapping", 51, true));
        game.session().keepArchive(unlocks);
        TurnReport report = TurnReport.build(me, me, List.of(
            new de.mkoehler.launchwindow.rules.GoalSettled("m9", "mars-mapping", false, 1_300, true),
            new de.mkoehler.launchwindow.rules.FindingPublished("lunar-mapping", 176, 2, 3, List.of()),
            new de.mkoehler.launchwindow.rules.FindingLearned("venus-atmosphere-study", List.of("Orion Spaceworks"),
                false)), 51, game.session().catalog());
        game.session().keepReport(report);
        write(folder, "findings-report.png", new TurnReportScreen(game, report), null);
        write(folder, "findings-dialog.png", new TurnReportScreen(game, report),
            screen -> screen.confirmPublish("mars-mapping"));
        write(folder, "findings-museum.png", new MuseumScreen(game, MuseumScreen.Tab.ARCHIVE), null);
        de.mkoehler.launchwindow.client.view.Archive archive = de.mkoehler.launchwindow.client.view.Archive.standard();
        write(folder, "findings-page.png", new ArchivePageScreen(game, de.mkoehler.launchwindow.client.view.Archive
            .entry(archive.entries(game.session().catalog(), unlocks), "mariner-9"), () -> null), null);
        write(folder, "findings-help.png", new MuseumScreen(game, MuseumScreen.Tab.ARCHIVE),
            screen -> screen.openHelp().select("findings"));

        String early = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        PlayerSnapshot planning = planningState(11, 25_000);
        planning.getAchievedGoals().put("organisms-in-microgravity", 12);
        startSession(game, planning, GameInfoResponse.success("early-era", early, List.of(), 1, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0));
        write(folder, "findings-plan.png", new MissionPlanScreen(game, "vostok-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);
    }

    /**
     * The archive (ui-spec.md 4.19): the Museum's tab with pages open, new and locked; a page with its
     * sound, flown by a rival; a page without one; the viewer; and the turn report's card with two new
     * pages and with one.
     */
    private static void archiveShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 52, true,
            6 * 3_600_000L + 12 * 60_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot me = new PlayerSnapshot("Baltic Star Agency", Archetype.NASA, 24_310, 14_280, 3_400, 52);
        me.setDirectorName("A. Reyes");
        startSession(game, me, info);
        // Luna 3, Ranger 7 and the spacewalks opened and read; Lunar Orbiter 1 (a rival) and Apollo 8 (you) new
        // this turn, 51; Mariner 4 too, later, for its own shot.
        game.useArchiveReadMarksInMemory(new java.util.HashSet<>(java.util.Set.of("luna-3", "ranger-7",
            "first-spacewalks")));
        List<de.mkoehler.launchwindow.net.messages.ArchiveUnlock> unlocks = List.of(
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("lunar-photos", 12, false),
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("lunar-close-up-photos", 30, true),
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("spacewalk", 38, false),
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("lunar-mapping", 51, false),
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("crewed-lunar-orbit", 51, true));
        game.session().keepArchive(unlocks);
        write(folder, "archive-museum.png", new MuseumScreen(game, MuseumScreen.Tab.ARCHIVE), null);
        de.mkoehler.launchwindow.client.view.Archive archive = de.mkoehler.launchwindow.client.view.Archive.standard();
        List<de.mkoehler.launchwindow.client.view.Archive.Entry> entries = archive.entries(game.session().catalog(),
            unlocks);
        de.mkoehler.launchwindow.client.view.Archive.Entry apollo8 =
            de.mkoehler.launchwindow.client.view.Archive.entry(entries, "apollo-8");
        write(folder, "archive-page-apollo-8.png", new ArchivePageScreen(game, apollo8, () -> new MuseumScreen(game,
            MuseumScreen.Tab.ARCHIVE)), null);
        write(folder, "archive-page-rival.png", new ArchivePageScreen(game,
            new de.mkoehler.launchwindow.client.view.Archive.Entry(apollo8.page(), 51, false), () -> null), null);
        write(folder, "archive-viewer.png", new ArchivePageScreen(game, apollo8, () -> null),
            screen -> screen.openViewer(1));
        for (String id : List.of("mariner-4", "pale-blue-dot", "venera-surface", "luna-3", "apollo-11", "apollo-15", "lunokhod-1",
            "venera-15-16", "pioneer-11", "surveyor-1", "luna-9",
            // The findings' pages (design.md 2.13, 2026-10-21).
            "explorer-1", "korabl-sputnik-2", "pegasus", "pioneer-6-9", "lunar-orbiter-micrometeoroids", "mascons",
            "surveyor-3", "mariner-5-venera-4", "zond-5", "mariners-6-7", "orbiting-frog-otolith", "viking-soil",
            "pioneer-venus-multiprobe")) {
            write(folder, "archive-page-" + id + ".png", new ArchivePageScreen(game,
                new de.mkoehler.launchwindow.client.view.Archive.Entry(archive.page(id), 60, true), () -> null), null);
        }
        TurnReport report = TurnReport.build(me, me, List.of(), 51, game.session().catalog());
        game.session().keepReport(report);
        write(folder, "archive-report.png", new TurnReportScreen(game, report), null);
        game.session().keepArchive(unlocks.subList(0, 4));
        write(folder, "archive-report-single.png", new TurnReportScreen(game, report), null);

        // The race decided in turn 51: the pages nobody reached open for everyone, and the report says so.
        List<de.mkoehler.launchwindow.net.messages.ArchiveUnlock> ended = new java.util.ArrayList<>(unlocks);
        for (String goal : List.of("mars-photos", "crewed-lunar-landing", "mars-mapping", "jupiter-photos",
            "venus-surface-study", "mars-orbiter-and-lander", "saturn-flyby")) {
            ended.add(new de.mkoehler.launchwindow.net.messages.ArchiveUnlock(goal, 51, false, true));
        }
        game.session().keepArchive(ended);
        write(folder, "archive-report-race-decided.png", new TurnReportScreen(game, report), null);
        write(folder, "archive-museum-race-decided.png", new MuseumScreen(game, MuseumScreen.Tab.ARCHIVE), null);
        write(folder, "archive-page-race-decided.png", new ArchivePageScreen(game, de.mkoehler.launchwindow.client.view
            .Archive.entry(archive.entries(game.session().catalog(), ended), "pale-blue-dot"), () -> null), null);
    }

    /** Whether new replay screens get the stand-in video player ({@link #fakeVideoPlayer}). */
    private static boolean fakeVideo;

    /**
     * A video player that plays nothing but always shows one frame of the launch clip, so the
     * replay console reads as live in the shots.
     */
    private static com.badlogic.gdx.video.VideoPlayer fakeVideoPlayer() {
        com.badlogic.gdx.graphics.Texture[] frame = new com.badlogic.gdx.graphics.Texture[1];
        return (com.badlogic.gdx.video.VideoPlayer) java.lang.reflect.Proxy.newProxyInstance(
            ScreenSnapshot.class.getClassLoader(), new Class<?>[]{com.badlogic.gdx.video.VideoPlayer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "load", "play", "update", "isBuffered", "isPlaying" ->
                    method.getReturnType() == boolean.class ? true : null;
                case "getTexture" -> {
                    if (frame[0] == null) {
                        frame[0] = new com.badlogic.gdx.graphics.Texture(Gdx.files.classpath("replay-frame.png"));
                        frame[0].setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                            com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
                    }
                    yield frame[0];
                }
                case "getVideoWidth" -> 960;
                case "getVideoHeight" -> 540;
                case "dispose" -> {
                    if (frame[0] != null) {
                        frame[0].dispose();
                    }
                    yield null;
                }
                default -> nothing(method.getReturnType());
            });
    }

    /** A stand-in's answer for a method it doesn't play: false, zero or nothing, by the return type. */
    private static Object nothing(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == float.class) {
            return 0f;
        }
        return null;
    }

    /**
     * The replay console (ui-spec.md 4.7.3, "The console", 2026-10-13) alone at the size of the
     * mockup's boards, one shot per state: watching, each outcome after its reveal, a saved glitch
     * mid-swing, a step reached with its result shown, no signal, paused; then the tallest and the
     * shortest console.
     */
    private static void consoleShots(File folder, LaunchWindowGame game) {
        record Shot(String name, MissionStepOutcome outcome, float time, boolean playing, boolean arrived,
                    boolean live, float height) {
        }
        float reveal = 3.2f;
        List<Shot> shots = List.of(
            new Shot("console-watching.png", MissionStepOutcome.SUCCESSFUL, 2f, true, false, true, 486f),
            new Shot("console-success.png", MissionStepOutcome.SUCCESSFUL, reveal + 2f, true, false, true, 486f),
            new Shot("console-glitch-saved-swing.png", MissionStepOutcome.MINOR_GLITCH_SOLVED, reveal + 0.6f, true,
                false, true, 486f),
            new Shot("console-glitch-saved.png", MissionStepOutcome.MINOR_GLITCH_SOLVED, reveal + 3f, true, false,
                true, 486f),
            new Shot("console-glitch.png", MissionStepOutcome.MINOR_GLITCH, reveal + 2f, true, false, true, 486f),
            new Shot("console-failure.png", MissionStepOutcome.FAILURE, reveal + 2f, true, false, true, 486f),
            new Shot("console-catastrophic.png", MissionStepOutcome.CATASTROPHIC, reveal + 2.1f, true, false, true,
                486f),
            new Shot("console-arrived-failure.png", MissionStepOutcome.FAILURE, 0f, true, true, true, 486f),
            new Shot("console-no-signal.png", MissionStepOutcome.SUCCESSFUL, 2f, true, false, false, 486f),
            new Shot("console-paused.png", MissionStepOutcome.SUCCESSFUL, reveal + 2f, false, false, true, 486f),
            new Shot("console-tallest.png", MissionStepOutcome.SUCCESSFUL, 2f, true, false, true, 520f),
            new Shot("console-shortest.png", MissionStepOutcome.SUCCESSFUL, 2f, true, false, true, 430f));
        ReplayClips.Clip launch = new ReplayClips.Clip("ui/replay/launch-success.webm", null, 6.3f, reveal, false,
            true, List.of());
        for (Shot shot : shots) {
            // No signal: no player, so the still shows, as on a step with no clip of its own.
            ReplayMonitor monitor = new ReplayMonitor(game.ui(), shot.live() ? fakeVideoPlayer() : null);
            monitor.play(shot.live() ? launch : ReplayClips.noSignal(), "Launch",
                ReplayConsole.live(ReplayConsole.seed("m-vostok", 2), shot.outcome(), reveal, shot.arrived()));
            monitor.setPlan(2, 11, "Vostok · Orbital flight");
            monitor.setTime(shot.time(), shot.playing(), 120f + shot.time());
            write(folder, shot.name(), new ConsoleShot(monitor, shot.height()), null);
            monitor.dispose();
        }
    }

    /**
     * A bare stage holding one replay console at a fixed size, on the page's blue.
     */
    private static final class ConsoleShot extends com.badlogic.gdx.ScreenAdapter {

        private final com.badlogic.gdx.scenes.scene2d.Stage stage = new com.badlogic.gdx.scenes.scene2d.Stage(
            new com.badlogic.gdx.utils.viewport.FitViewport(Theme.VIEW_WIDTH, Theme.VIEW_HEIGHT));

        ConsoleShot(ReplayMonitor monitor, float height) {
            com.badlogic.gdx.scenes.scene2d.ui.Table root = new com.badlogic.gdx.scenes.scene2d.ui.Table();
            root.setFillParent(true);
            root.add(monitor).size(1086f, height);
            stage.addActor(root);
        }

        @Override
        public void render(float delta) {
            Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            stage.act(delta);
            stage.draw();
        }

        @Override
        public void resize(int width, int height) {
            stage.getViewport().update(width, height, true);
        }

        @Override
        public void dispose() {
            stage.dispose();
        }
    }

    /**
     * Funding raised between reviews (design.md 2.1(f), 2026-10-12): the turn report's budget card,
     * and the Public Affairs Office after a raise, with its line on what more prestige would add.
     */
    /**
     * Every sheet of every explainer (ui-spec.md component 18), alone with {@code -Dshots=explainers}.
     */
    private static void explainerShots(File folder, LaunchWindowGame game) {
        for (Explainers.Explainer explainer : Explainers.standard().explainers()) {
            for (int sheet = 0; sheet < explainer.sheets().size(); sheet++) {
                write(folder, "explainer-" + explainer.code().toLowerCase(java.util.Locale.ROOT) + "-" + (sheet + 1)
                    + ".png", new ExplainerScreen(game, explainer, sheet, () -> new MuseumScreen(game,
                    MuseumScreen.Tab.ENCYCLOPEDIA)), null);
            }
        }
        // The explainers that answer a theory of the day, locked until it's settled (2026-10-27), then open.
        write(folder, "museum-encyclopedia-locked.png", new MuseumScreen(game, MuseumScreen.Tab.ENCYCLOPEDIA), null);
        game.session().keepFrontPages(java.util.stream.Stream.of("weightless-body", "radiation-belts", "deep-dust",
            "venus-jungle").map(theory -> new FrontPage("1-science-" + theory, "goal", 1, theory, "", List.of(),
                List.of(), List.of(), FrontPage.Kind.SCIENCE, null, null)).toList());
        write(folder, "museum-encyclopedia-settled.png", new MuseumScreen(game, MuseumScreen.Tab.ENCYCLOPEDIA), null);
    }

    /**
     * The Join screen's Randomize button (ui-spec.md 4.2): a suggested name filled in (set directly,
     * since the button deals at random), and the button's tooltip.
     */
    private static void joinNameShots(File folder, LaunchWindowGame game) {
        write(folder, "join-randomized.png", new JoinScreen(game, CREDENTIALS), screen -> {
            screen.selectArchetype(Archetype.SOVIET);
            screen.agencyNameField().input().setText("Kibalchich Design Bureau");
        });
        write(folder, "join-randomize-tooltip.png", new JoinScreen(game, CREDENTIALS),
            screen -> showTooltip(screen.stage().getRoot(), "Suggest a name"));
    }

    private static void raiseShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 1, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot raised = planningState(11, 25_000);
        raised.setSeason(21);
        raised.setPrestigeAtLastReview(1_400);
        raised.setFundedPrestige(2_140);
        raised.setFundingRatePerSeason(6_020);
        startSession(game, raised, info);
        write(folder, "raise-pao.png", new PublicAffairsScreen(game), null);
        TurnReport report = TurnReport.build(raised, raised, List.of(
            new de.mkoehler.launchwindow.rules.FundingRaised(1, 2_140, 4_620, 6_020),
            new de.mkoehler.launchwindow.rules.SeasonFundsSettled(5_100, 6_020, 25_000, null)), 21,
            game.session().catalog());
        game.session().keepReport(report);
        write(folder, "raise-report.png", new TurnReportScreen(game, report), null);
    }

    /**
     * The answer to rivals' Firsts (design.md 2.1(e), 2026-10-12): the turn report's budget card with
     * the government's answer, one where the same season's review counted it, and the Public Affairs
     * Office's review card with a credit waiting.
     */
    private static void rivalFirstsShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 1, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot behind = planningState(11, 25_000);
        behind.setRivalFirstsCredit(2_375);
        startSession(game, behind, info);
        write(folder, "rival-firsts-pao.png", new PublicAffairsScreen(game), null);
        write(folder, "rival-firsts-help.png", new PublicAffairsScreen(game),
            screen -> screen.openHelp().select("budget"));
        TurnReport report = TurnReport.build(behind, behind, List.of(
            new de.mkoehler.launchwindow.rules.RivalFirstsAnswered(List.of("human-in-orbit", "human-in-space"), 1_500,
                2_375),
            new de.mkoehler.launchwindow.rules.SeasonFundsSettled(9_800, 10_800, 25_000, null)), 22,
            game.session().catalog());
        game.session().keepReport(report);
        write(folder, "rival-firsts-report.png", new TurnReportScreen(game, report), null);
        TurnReport reviewed = TurnReport.build(behind, behind, List.of(
            new de.mkoehler.launchwindow.rules.RivalFirstsAnswered(List.of("human-in-orbit"), 1_500, 2_375),
            new de.mkoehler.launchwindow.rules.BudgetReviewed(2, 5_900, 0, 12_700, 2_375),
            new de.mkoehler.launchwindow.rules.SeasonFundsSettled(9_800, 12_700, 25_000, null)), 32,
            game.session().catalog());
        game.session().keepReport(reviewed);
        write(folder, "rival-firsts-review.png", new TurnReportScreen(game, reviewed), null);
    }

    /**
     * Crewed flights grounded after a crew was lost (design.md 2.5(j), 2026-10-12): the launch screen's
     * refusal, the mission cards' readiness, and the turn report's line; then the advisor's idle tip
     * (2026-10-12), on the retired advisor and on top of the first steps.
     */
    private static void groundingShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 1, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot grounded = planningState(11, 25_000);
        grounded.setCrewedGroundingSeasons(3);
        startSession(game, grounded, info);
        write(folder, "grounding-assemble.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        write(folder, "grounding-configs.png", new MissionConfigsScreen(game, "vostok"), null);
        TurnReport report = TurnReport.build(grounded, grounded, List.of(
            new de.mkoehler.launchwindow.rules.CrewedFlightsGrounded("m3", "vostok-orbital", 4),
            new de.mkoehler.launchwindow.rules.GoalsWithheld("m3", "vostok-orbital",
                List.of("human-in-orbit", "human-in-space"), List.of("human-in-orbit"))), 22,
            game.session().catalog());
        game.session().keepReport(report);
        write(folder, "grounding-report.png", new TurnReportScreen(game, report), null);
        // A crewed flight's goals count only if the crew comes home (design.md 2.6).
        write(folder, "grounding-steps.png", new MissionStepsScreen(game, "vostok-orbital"), null);

        java.util.Set<String> marks = new java.util.HashSet<>(java.util.Set.of("helpOpened", "advisorRetired"));
        game.useAdvisorMarksInMemory(marks);
        PlayerSnapshot idle = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 23_350, 250, 3_000, 1);
        idle.setDirectorName("A. Reyes");
        SetSpecialist working = new SetSpecialist("e1", "R. Hale", 34, 70, 50);
        working.setRdTargetComponentId("explorer-1-satellite");
        working.setRdSlot(0);
        working.setAssignmentState(de.mkoehler.launchwindow.model.AssignmentState.ASSIGNED);
        idle.getSetSpecialists().add(working);
        idle.getSetSpecialists().add(new SetSpecialist("e2", "M. Brandt", 29, 70, 50));
        idle.getOpenedProgramIds().add("explorer-1");
        idle.getMissionComponents().put("explorer-1-satellite", new OwnedMissionComponent(41.7, 3));
        startSession(game, idle, info);
        write(folder, "advisor-idle-tip.png", new SpaceComplexScreen(game), null);
        marks.clear();
        marks.add("helpOpened");
        startSession(game, idle, info);
        write(folder, "advisor-idle-tip-first-steps.png", new SpaceComplexScreen(game), null);
        game.useAdvisorMarksInMemory(new java.util.HashSet<>(java.util.Set.of("idleTipDone")));
    }

    /**
     * The Firsts' front pages (ui-spec.md section 4.18, 2026-10-11) in the standard pack: one per First,
     * a rival's page and this player's, a tie, a page with an earlier First folded in, the first of
     * two, and the longest names allowed.
     */
    private static void frontPageShots(File folder, LaunchWindowGame game) {
        game.useFrontPageMarksInMemory(new java.util.HashSet<>());
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 27, true,
            6 * 3_600_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot me = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 60_000, 2_140, 3_000, 27);
        me.setDirectorName("A. Reyes");
        startSession(game, me, info);
        FrontPage.Achiever orion = new FrontPage.Achiever("Orion Spaceworks", "Ana Reyes", List.of("Daniel Hale"));
        FrontPage.Achiever kestrel = new FrontPage.Achiever("Kestrel Aerospace", "Moses Okafor", List.of());
        FrontPage.Achiever baltic = new FrontPage.Achiever("Baltic Star Agency", "A. Reyes", List.of("Pavel Brandt"));
        FrontPage satellite = new FrontPage("11-earth-orbit-satellite", "earth-orbit-satellite", 11, "sat-new-moon",
            "Crisp, clear evening", List.of(), List.of(kestrel), List.of("Orion Spaceworks", "Baltic Star Agency"));
        FrontPage space = new FrontPage("25-human-in-space", "human-in-space", 25, "space-first-flight",
            "Snow flurries, bitter cold", List.of(), List.of(baltic), List.of("Orion Spaceworks", "Kestrel Aerospace"));
        FrontPage orbit = new FrontPage("26-human-in-orbit", "human-in-orbit", 26, "orbit-circles",
            "Fair and cool tonight", List.of(), List.of(orion), List.of("Kestrel Aerospace", "Baltic Star Agency"));
        List<FrontPage> all = List.of(satellite, space, orbit);
        game.session().keepFrontPages(all);
        write(folder, "frontpage-orbit-rival.png", new FrontPageScreen(game, List.of(orbit), () -> { }, false), null);
        write(folder, "frontpage-two.png", new FrontPageScreen(game, List.of(space, orbit), () -> { }, false), null);
        write(folder, "frontpage-space-you.png", new FrontPageScreen(game, List.of(space), () -> { }, true), null);
        write(folder, "frontpage-satellite.png", new FrontPageScreen(game, List.of(satellite), () -> { }, true), null);

        // Later Firsts: a flyby with human in orbit folded in, then the landing as a tie.
        FrontPage flyby = new FrontPage("40-crewed-lunar-flyby", "crewed-lunar-flyby", 40, "flyby-earthrise",
            "Windy with rain at times", List.of(), new java.util.ArrayList<>(List.of(new FrontPage.Achiever(
                "Kestrel Aerospace", "Moses Okafor", List.of("Ruth Lindqvist", "Tomas Ortiz", "Grace Mbeki")))),
            List.of("Orion Spaceworks"));
        FrontPage landing = new FrontPage("52-crewed-lunar-landing", "crewed-lunar-landing", 52, "landing-tie",
            "Hot and humid", List.of(), List.of(new FrontPage.Achiever("Orion Spaceworks", "Ana Reyes",
                List.of("Daniel Hale", "Lena Fischer")), new FrontPage.Achiever("Baltic Star Agency", "A. Reyes",
                List.of("Pavel Brandt", "Irina Sokolova"))), List.of("Kestrel Aerospace"));
        game.session().keepFrontPages(List.of(satellite, space, orbit, flyby, landing));
        write(folder, "frontpage-flyby.png", new FrontPageScreen(game, List.of(flyby), () -> { }, true), null);
        write(folder, "frontpage-landing-tie.png", new FrontPageScreen(game, List.of(landing), () -> { }, true), null);
        FrontPage landingAlone = new FrontPage("52-crewed-lunar-landing", "crewed-lunar-landing", 52,
            "landing-footprints", "Hot and humid", List.of(), List.of(new FrontPage.Achiever("Orion Spaceworks",
                "Ana Reyes", List.of("Daniel Hale", "Lena Fischer"))), List.of("Kestrel Aerospace", "Baltic Star Agency"));
        write(folder, "frontpage-landing.png", new FrontPageScreen(game, List.of(landingAlone), () -> { }, true), null);

        // A first orbit that also took human in space, and the longest names allowed (32 characters).
        FrontPage folded = new FrontPage("26-human-in-orbit", "human-in-orbit", 26, "orbit-around-the-world",
            "Fair and cool tonight", List.of("earth-orbit-satellite", "human-in-space"), List.of(
                new FrontPage.Achiever("Interplanetary Research Agency X", "Alexandra Konstantinopoulou-Sims",
                    List.of("Bartholomew Fairweather-Hughes"))),
            List.of("Consolidated Rocket Works Bureau", "Northern Lights Space Directorate"));
        game.session().keepFrontPages(List.of(folded));
        write(folder, "frontpage-long-names.png", new FrontPageScreen(game, List.of(folded), () -> { }, false), null);

        // The scrim over the brightest hub art (Soviet winter noon) and the darkest (winter night).
        startSession(game, me, GameInfoResponse.success("standard", hash, List.of(), 25, true, 6 * 3_600_000L, 0L,
            null, List.of(), List.of(), 0));
        game.session().keepFrontPages(all);
        write(folder, "frontpage-winter-noon.png", new FrontPageScreen(game, List.of(space), () -> { }, true), null);
        long later = System.currentTimeMillis();
        while (game.hubClock().current() != Hub.TimeOfDay.NIGHT) {
            later += 24 * 3_600_000L;
            game.hubClock().enter(later);
        }
        write(folder, "frontpage-winter-night.png", new FrontPageScreen(game, List.of(space), () -> { }, true), null);
        while (game.hubClock().current() != Hub.TimeOfDay.NOON) {
            later += 24 * 3_600_000L;
            game.hubClock().enter(later);
        }
    }

    /**
     * In memory (ui-spec.md section 4.18.3, component 29, 2026-10-22): the memorial page with three crew, a quote
     * and two condolences; a short one; the losing agency's own copy; the eulogy dialog fresh and with a tone; the
     * condolence form.
     */
    /**
     * The guided progression (design.md 2.3(d), 2026-10-26), alone with {@code -Dshots=progression}: the advisor's
     * road ahead after the first satellite and after a person in space, and rocket-plane wings on the roster, the
     * person page and the mission plan's steps.
     */
    private static void progressionShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 6, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        game.useAdvisorMarksInMemory(new java.util.HashSet<>(java.util.Set.of("idleTipDone", "helpOpened",
            "reportRead")));
        PlayerSnapshot nasa = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 21_400, 1_150, 3_000, 6);
        nasa.setDirectorName("A. Reyes");
        SetSpecialist engineer = new SetSpecialist("e1", "R. Hale", 34, 70, 50);
        engineer.setRdTargetComponentId("explorer-1-satellite");
        nasa.getSetSpecialists().add(engineer);
        nasa.getMissionControlOfficers().add(new MissionControlOfficer("c1", "J. Ortiz", 31, 70, 50));
        nasa.getOpenedProgramIds().add("explorer-1");
        nasa.getMissionComponents().put("explorer-1-satellite", new OwnedMissionComponent(72.0, 3.0));
        nasa.getMissionComponents().put("jupiter-c", new OwnedMissionComponent(68.0, 3.0));
        nasa.setLastFlightSeasonTurn(5);
        nasa.getAchievedGoals().put("earth-orbit-satellite", 5);
        startSession(game, nasa, info);
        write(folder, "progression-road-ahead.png", new SpaceComplexScreen(game), null);
        write(folder, "progression-hq-moon.png", new BodyProgramsScreen(game, CelestialBody.MOON), null);

        Astronaut walker = new Astronaut("a1", "J. Walker", 34, 80, 50);
        Astronaut glenn = new Astronaut("a2", "J. Glenn", 37, 80, 50);
        for (AstronautSkill skill : AstronautSkill.values()) {
            walker.setSkill(skill, 55.0);
            glenn.setSkill(skill, 50.0);
        }
        walker.setRocketPlaneWings(true);
        nasa.getAstronauts().addAll(List.of(walker, glenn));
        nasa.getOpenedProgramIds().addAll(List.of("mercury", "x-15"));
        nasa.getMissionComponents().put("mercury-capsule", new OwnedMissionComponent(74.0, 3.0));
        nasa.getMissionComponents().put("x-15-aircraft", new OwnedMissionComponent(70.0, 3.0));
        nasa.getMissionComponents().put("redstone", new OwnedMissionComponent(71.0, 3.0));
        nasa.getAchievedGoals().put("human-in-space", 9);
        startSession(game, nasa, info);
        write(folder, "progression-road-ahead-capsules.png", new SpaceComplexScreen(game), null);
        write(folder, "progression-wings-roster.png", new AstronautComplexScreen(game), null);
        write(folder, "progression-wings-person.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game,
            walker, () -> new AstronautComplexScreen(game)), null);
        write(folder, "progression-wings-plan.png", new MissionPlanScreen(game, "mercury-suborbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);
        game.useAdvisorMarksInMemory(new java.util.HashSet<>(java.util.Set.of("idleTipDone")));
    }

    /**
     * Roads to the Moon (ui-spec.md 4.21, 2026-10-26), alone with {@code -Dshots=roads}: the canvas's states. NASA
     * with a person in space and Mercury open; with Gemini open; with Apollo opened; the Soviet roads; locked before a
     * person has flown in space; a history note pinned; and Headquarters' Moon page with its button.
     */
    private static void roadsShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 34, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot nasa = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 31_400, 4_820, 3_000, 34);
        nasa.setDirectorName("A. Reyes");
        nasa.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 2);
        nasa.getOpenedProgramIds().addAll(List.of("explorer-1", "mercury"));
        nasa.getMissionComponents().put("explorer-1-satellite", new OwnedMissionComponent(81.0, 3.0));
        nasa.getMissionComponents().put("mercury-capsule", new OwnedMissionComponent(74.0, 3.0));
        nasa.getAchievedGoals().put("earth-orbit-satellite", 6);
        nasa.getAchievedGoals().put("human-in-space", 30);
        startSession(game, nasa, info);
        write(folder, "roads-nasa.png", new RoadsToMoonScreen(game), null);
        write(folder, "roads-pinned-note.png", new RoadsToMoonScreen(game), screen -> screen.pinHistoryNote(0));
        BodyProgramsScreen moon = new BodyProgramsScreen(game, CelestialBody.MOON);
        moon.select("lunar-expeditions");
        write(folder, "roads-hq-moon.png", moon, null);
        write(folder, "roads-program-buttons.png", new de.mkoehler.launchwindow.client.screen.ProgramDetailsScreen(game,
            "apollo-earth-orbit", CelestialBody.EARTH), null);

        nasa.getAchievedGoals().remove("human-in-space");
        startSession(game, nasa, info);
        write(folder, "roads-locked.png", new RoadsToMoonScreen(game), null);
        nasa.getAchievedGoals().put("human-in-space", 30);

        nasa.getOpenedProgramIds().add("gemini");
        nasa.getMissionComponents().put("gemini-capsule", new OwnedMissionComponent(64.0, 3.0));
        startSession(game, nasa, info);
        write(folder, "roads-gemini-open.png", new RoadsToMoonScreen(game), null);

        nasa.setFunds(18_200);
        nasa.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 3);
        nasa.getOpenedProgramIds().addAll(List.of("apollo-earth-orbit", "apollo"));
        nasa.getMissionComponents().put("apollo-csm", new OwnedMissionComponent(38.0, 3.0));
        nasa.getMissionComponents().put("apollo-lunar-module", new OwnedMissionComponent(21.0, 3.0));
        nasa.getMissionComponents().put("apollo-lunar-eva-suit", new OwnedMissionComponent(30.0, 3.0));
        nasa.getMissionComponents().put("lunar-roving-vehicle", new OwnedMissionComponent(15.0, 3.0));
        nasa.getMissionComponents().put("agena-target-vehicle", new OwnedMissionComponent(58.0, 3.0));
        nasa.getAchievedGoals().put("rendezvous", 40);
        nasa.getAchievedGoals().put("docking", 42);
        startSession(game, nasa, info);
        write(folder, "roads-apollo-chosen.png", new RoadsToMoonScreen(game), null);

        PlayerSnapshot soviet = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 30_000, 4_820, 3_000, 34);
        soviet.setDirectorName("A. Reyes");
        soviet.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 2);
        soviet.getOpenedProgramIds().addAll(List.of("sputnik-1", "vostok"));
        soviet.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(81.0, 3.0));
        soviet.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(74.0, 3.0));
        soviet.getAchievedGoals().put("earth-orbit-satellite", 5);
        soviet.getAchievedGoals().put("human-in-space", 28);
        startSession(game, soviet, info);
        write(folder, "roads-soviet.png", new RoadsToMoonScreen(game), null);
    }

    private static void memorialShots(File folder, LaunchWindowGame game) {
        game.useFrontPageMarksInMemory(new java.util.HashSet<>());
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 44, true,
            6 * 3_600_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot rival = new PlayerSnapshot("Northern Rocket Bureau", Archetype.NASA, 60_000, 9_640, 3_000, 44);
        rival.setDirectorName("Ellen Hale");
        startSession(game, rival, info);
        FrontPage.Loss loss = new FrontPage.Loss("43-loss-1", 43, "k-orion", "Orion Spaceworks", "Clara Voss",
            "gemini-spacewalk", "18-reentry", List.of(
                new FrontPage.Lost("Ana Reyes", 38, "nasa_001", "a1", 2, 0, 28, List.of("MENTOR")),
                new FrontPage.Lost("Moses Okafor", 35, "nasa_002", "a2", 1, 0, 20, List.of("EASY_GOING")),
                new FrontPage.Lost("Lev Marek", 32, "nasa_003", "a3", 1, 0, 6, List.of())),
            List.of("gemini-spacewalk-flown", "spacewalk"), List.of("spacewalk"));
        FrontPage.Achiever orion = new FrontPage.Achiever("Orion Spaceworks", "Clara Voss",
            List.of("Ana Reyes", "Moses Okafor", "Lev Marek"));
        FrontPage three = new FrontPage("44-memorial-43-loss-1", "", 44, "m-people-1", "Snow flurries, bitter cold",
            List.of(), List.of(orion), List.of(), FrontPage.Kind.MEMORIAL, null, List.of(
                new FrontPage.Reaction("Northern Rocket Bureau", "Ellen Hale", FrontPage.Stance.TRIBUTE,
                    "They went first, and they showed us the way. We will not forget them.", "m-tribute-q-1", null, null),
                new FrontPage.Reaction("Polaris Space Agency", "Ruth Calder", FrontPage.Stance.CONDOLENCES, "",
                    "m-condolences-1", null, null)),
            new FrontPage.Memorial(loss, FrontPage.Stance.PEOPLE, "Ana sang on the radio when she thought no one was "
                + "listening. Moses never lost at chess. Lev wrote home every day.", List.of("m-people-1")));
        FrontPage.Loss launch = new FrontPage.Loss("43-loss-2", 43, "k-orion", "Orion Spaceworks", "Clara Voss",
            "gemini-docking", "03-launch", List.of(
                new FrontPage.Lost("Jonas Lindqvist", 41, "nasa_004", "a4", 3, 1, 40, List.of()),
                new FrontPage.Lost("Hanna Weber", 41, "nasa_005", "a5", 2, 0, 30, List.of())), List.of(), List.of());
        FrontPage two = new FrontPage("44-memorial-43-loss-2", "", 44, "m-courage-1", "Snow flurries, bitter cold",
            List.of(), List.of(new FrontPage.Achiever("Orion Spaceworks", "Clara Voss", List.of())), List.of(),
            FrontPage.Kind.MEMORIAL, null, List.of(), new FrontPage.Memorial(launch, FrontPage.Stance.COURAGE, "",
                List.of("m-courage-1")));
        game.session().keepFrontPages(List.of(three, two));
        game.session().keepLosses(List.of(loss));
        game.session().keepStatements(List.of());
        write(folder, "memorial-page.png", new FrontPageScreen(game, List.of(three), () -> { }, false), null);
        write(folder, "memorial-short.png", new FrontPageScreen(game, List.of(two), () -> { }, false), null);
        write(folder, "memorial-help.png", new FrontPageScreen(game, List.of(two), () -> { }, false),
            screen -> screen.openHelp());
        // The fullest page: the director's words and four rivals' at the longest, a fifth folded in; it runs on.
        String longest = "We grieve with you. Your crew were the finest of us all, and every agency in this race owes "
            + "them a debt that we will never be able";
        List<FrontPage.Reaction> crowd = new java.util.ArrayList<>();
        for (String name : List.of("Northern Rocket Bureau", "Polaris Space Agency", "Kestrel Space Directorate",
                "Baltic Star Agency")) {
            crowd.add(new FrontPage.Reaction(name, "Konstantin Alexandropoulos", FrontPage.Stance.CONDOLENCES, longest,
                "m-condolences-q-1", null, null));
        }
        crowd.add(new FrontPage.Reaction("Meridian Aerospace", "Ana Ruiz", FrontPage.Stance.TRIBUTE, "", "m-also-1",
            null, null));
        FrontPage crowded = new FrontPage("44-memorial-43-loss-3", "", 44, "m-people-2", "Snow flurries, bitter cold",
            List.of(), List.of(orion), List.of(), FrontPage.Kind.MEMORIAL, null, crowd,
            new FrontPage.Memorial(loss, FrontPage.Stance.PEOPLE, longest, List.of("m-people-2")));
        write(folder, "memorial-crowded.png", new FrontPageScreen(game, List.of(crowded), () -> { }, false), null);
        write(folder, "memorial-crowded-2.png", new FrontPageScreen(game, List.of(crowded), () -> { }, false),
            FrontPageScreen::showContinuation);
        TurnReport rivalReport = TurnReport.build(rival, rival, List.of(), 43, game.session().catalog());
        game.session().keepReport(rivalReport);
        write(folder, "memorial-report-rival.png", new TurnReportScreen(game, rivalReport), null);
        write(folder, "memorial-condolences.png", new FrontPageScreen(game, List.of(three), () -> { }, false),
            screen -> StatementDialog.condolences(screen, loss, () -> { }).preset(FrontPage.Stance.TRIBUTE,
                "They went first, and they showed us the way. We will not forget them.").show());

        // The losing agency: its own copy, and the statement it gives the turn after the loss.
        PlayerSnapshot own = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 16_940, 7_210, 3_000, 44);
        own.setDirectorName("Clara Voss");
        for (FrontPage.Lost lost : loss.crew()) {
            own.getMemorial().add(new de.mkoehler.launchwindow.model.MemorialEntry(lost.name(), lost.age(), 43,
                de.mkoehler.launchwindow.model.MemorialEntry.Cause.MISSION, "gemini-spacewalk", "18-reentry",
                lost.portraitId(), lost.personnelId(), java.util.Map.of(), List.of(), 15, List.of()));
        }
        startSession(game, own, info);
        game.session().keepFrontPages(List.of(three, two));
        game.session().keepLosses(List.of(loss));
        game.session().keepStatements(List.of());
        write(folder, "memorial-own.png", new FrontPageScreen(game, List.of(three), () -> { }, false), null);
        TurnReport ownReport = TurnReport.build(own, own, List.of(
            new de.mkoehler.launchwindow.rules.CrewedFlightsGrounded("m7", "gemini-spacewalk", 4),
            new de.mkoehler.launchwindow.rules.GoalsWithheld("m7", "gemini-spacewalk", List.of("spacewalk"),
                List.of("spacewalk"))), 43, game.session().catalog());
        game.session().keepReport(ownReport);
        write(folder, "memorial-report-own.png", new TurnReportScreen(game, ownReport), null);
        write(folder, "memorial-hub.png", new SpaceComplexScreen(game), null);
        write(folder, "memorial-person.png", new de.mkoehler.launchwindow.client.screen.MemorialPersonScreen(game, 0, () -> new SpaceComplexScreen(game)), null);
        write(folder, "memorial-eulogy-fresh.png", new FrontPageScreen(game, List.of(three), () -> { }, false),
            screen -> StatementDialog.eulogy(screen, loss, () -> { }).show());
        write(folder, "memorial-eulogy-picked.png", new FrontPageScreen(game, List.of(three), () -> { }, false),
            screen -> StatementDialog.eulogy(screen, loss, () -> { }).preset(FrontPage.Stance.CAUSE,
                "We owe them the truth. We will find what went wrong, fix it, and fly again.").show());
    }

    /**
     * Theories of the day (ui-spec.md section 4.18.2, 2026-10-22): a turn report raising three theories, one of them
     * settled already, with a public and a technical theory settled this turn; and two science pages.
     */
    private static void theoryShots(File folder, LaunchWindowGame game) {
        game.useFrontPageMarksInMemory(new java.util.HashSet<>());
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 31, true,
            6 * 3_600_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot me = new PlayerSnapshot("Baltic Star Agency", Archetype.NASA, 40_000, 3_100, 3_000, 31);
        me.setDirectorName("A. Reyes");
        me.getAchievedGoals().put("earth-orbit-satellite", 5);
        me.getAchievedGoals().put("human-in-space", 12);
        me.getAchievedGoals().put("lunar-probe", 30);
        startSession(game, me, info);
        FrontPage.Achiever orion = new FrontPage.Achiever("Orion Spaceworks", "Ana Reyes", List.of());
        FrontPage.Achiever kestrel = new FrontPage.Achiever("Kestrel Aerospace", "Moses Okafor", List.of());
        FrontPage mars = new FrontPage("28-science-mars-canals", "mars-probe", 28, "mars-canals", "Hot and humid",
            List.of(), List.of(orion), List.of(), FrontPage.Kind.SCIENCE, null, null);
        FrontPage belts = new FrontPage("30-science-radiation-belts", "magnetic-field-study", 30, "radiation-belts",
            "Crisp, clear evening", List.of(), List.of(orion), List.of(), FrontPage.Kind.SCIENCE, null, null);
        FrontPage grit = new FrontPage("30-science-micrometeoroids", "micrometeoroid-study", 30, "micrometeoroids",
            "Crisp, clear evening", List.of(), List.of(kestrel), List.of(), FrontPage.Kind.SCIENCE, null, null);
        FrontPage dust = new FrontPage("34-science-deep-dust", "lunar-lander", 34, "deep-dust", "Fair and cool tonight",
            List.of(), List.of(kestrel, orion), List.of(), FrontPage.Kind.SCIENCE, null, null);
        game.session().keepFrontPages(List.of(mars, belts, grit, dust));
        TurnReport report = TurnReport.build(me, me, List.of(
            new de.mkoehler.launchwindow.rules.GoalSettled("m3", "lunar-probe", false, 800, false)), 30,
            game.session().catalog());
        game.session().keepReport(report);
        write(folder, "theories-report.png", new TurnReportScreen(game, report), null);
        write(folder, "theories-page-mars.png", new FrontPageScreen(game, List.of(mars), () -> { }, false), null);
        write(folder, "theories-page-dust.png", new FrontPageScreen(game, List.of(dust), () -> { }, false), null);
        // The Museum's list (raised and settled, with the open archive pages) and a page reopened from it.
        game.useArchiveReadMarksInMemory(new java.util.HashSet<>());
        game.session().keepArchive(List.of(
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("lunar-surface-photos", 33, false),
            new de.mkoehler.launchwindow.net.messages.ArchiveUnlock("mars-photos", 28, false)));
        write(folder, "theories-museum.png", new MuseumScreen(game, MuseumScreen.Tab.ARCHIVE), null);
        write(folder, "theories-page-reopened.png", new FrontPageScreen(game, List.of(mars), () -> { }, true), null);
    }

    /**
     * The world reacts (ui-spec.md section 4.18.1, component 28, 2026-10-22): a rival's First with the
     * strip's statement button in its states, the statement dialog's states, and follow-up pages for one
     * statement, a few, and a crowded game at the longest names, with its continuation sheet.
     */
    private static void reactionShots(File folder, LaunchWindowGame game) {
        game.useFrontPageMarksInMemory(new java.util.HashSet<>());
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 26, true,
            6 * 3_600_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot me = new PlayerSnapshot("Northern Rocket Bureau", Archetype.NASA, 60_000, 2_140, 3_000, 26);
        me.setDirectorName("Ellen Hale");
        startSession(game, me, info);
        FrontPage satellite = new FrontPage("11-earth-orbit-satellite", "earth-orbit-satellite", 11, "sat-new-moon",
            "Crisp, clear evening", List.of(), List.of(new FrontPage.Achiever("Northern Rocket Bureau", "Ellen Hale",
                List.of())), List.of("Orion Spaceworks"));
        FrontPage space = new FrontPage("25-human-in-space", "human-in-space", 25, "space-up-and-down",
            "Fair and cool tonight", List.of(), List.of(new FrontPage.Achiever("Kestrel Space Directorate",
                "Mikhail Sorin", List.of("Lev Marek"))), List.of("Northern Rocket Bureau", "Polaris Space Agency"));
        game.session().keepFrontPages(List.of(satellite, space));
        game.session().keepStatements(List.of());
        write(folder, "reactions-strip-give.png", new FrontPageScreen(game, List.of(space), () -> { }, false), null);
        write(folder, "reactions-reminder.png", new FrontPageScreen(game, List.of(space), () -> { }, false),
            FrontPageScreen::leaveUnanswered);
        write(folder, "reactions-dialog-fresh.png", new FrontPageScreen(game, List.of(space), () -> { }, false),
            screen -> new StatementDialog(screen, space, () -> { }).show());
        write(folder, "reactions-dialog-next.png", new FrontPageScreen(game, List.of(space), () -> { }, false),
            screen -> new StatementDialog(screen, space, () -> { }).preset(FrontPage.Stance.NEXT,
                "Congratulations to Kestrel on a fine flight. Our pilots are ready, and we will be up soon.").show());
        write(folder, "reactions-dialog-nocomment.png", new FrontPageScreen(game, List.of(space), () -> { }, false),
            screen -> new StatementDialog(screen, space, () -> { }).preset(FrontPage.Stance.NO_COMMENT, "").show());
        write(folder, "reactions-dialog-error.png", new FrontPageScreen(game, List.of(space), () -> { }, false),
            screen -> new StatementDialog(screen, space, () -> { }).preset(FrontPage.Stance.POLITE,
                "Well flown <3").show());
        game.session().keepStatements(List.of(new FrontPage.Statement(space.id(), FrontPage.Stance.POLITE,
            "Well flown.")));
        write(folder, "reactions-strip-given.png", new FrontPageScreen(game, List.of(space), () -> { }, false), null);
        write(folder, "reactions-dialog-edit.png", new FrontPageScreen(game, List.of(space), () -> { }, false),
            screen -> new StatementDialog(screen, space, () -> { }).show());

        // The follow-ups, printed by the server's own printer from the bundled words.
        FrontPageReactions words = FrontPageReactions.standard();
        List<ReactionPrinter.Agency> few = List.of(
            agencyOf("Kestrel Space Directorate", "Mikhail Sorin", null, ""),
            agencyOf("Northern Rocket Bureau", "Ellen Hale", FrontPage.Stance.ENTHUSIASTIC,
                "Today belongs to everyone who has ever looked up."),
            agencyOf("Halberd Rocket Bureau", "Oskar Lind", FrontPage.Stance.POLITE, ""),
            agencyOf("Polaris Space Agency", "Ruth Calder", FrontPage.Stance.NEXT,
                "We will be next, and we will stay up longer."),
            agencyOf("Orion Spaceworks", "Sam Okafor", null, ""));
        FrontPage fewPage = ReactionPrinter.print(26, 7L, List.of(space), few, words, FrontPages.standard()).get(0);
        FrontPage one = ReactionPrinter.print(26, 7L, List.of(space), List.of(few.get(0), few.get(3)), words,
            FrontPages.standard()).get(0);
        startSession(game, me, GameInfoResponse.success("standard", hash, List.of(), 27, true,
            6 * 3_600_000L, 0L, null, List.of(), List.of(), 0));
        game.session().keepFrontPages(List.of(satellite, space, fewPage));
        write(folder, "reactions-page-few.png", new FrontPageScreen(game, List.of(fewPage), () -> { }, false), null);
        write(folder, "reactions-strip-read.png", new FrontPageScreen(game, List.of(space), () -> { }, true), null);
        game.session().keepFrontPages(List.of(satellite, space, one));
        write(folder, "reactions-page-one.png", new FrontPageScreen(game, List.of(one), () -> { }, false), null);

        // A crowded game at the longest names and quotes allowed: four told, the rest folded, a continuation.
        String quote = ("We congratulate them warmly on a fine achievement and we look forward to the day when our "
            + "own crews follow them into the history books. ").repeat(2).substring(0,
            FrontPage.Statement.MAX_QUOTE_LENGTH).strip();
        List<ReactionPrinter.Agency> crowd = new java.util.ArrayList<>();
        crowd.add(agencyOf("Kestrel Space Directorate", "Mikhail Sorin", null, ""));
        String[] names = {"Interplanetary Research Agency A", "Interplanetary Research Agency B",
            "Interplanetary Research Agency C", "Interplanetary Research Agency D", "Interplanetary Research Agency E",
            "Interplanetary Research Agency F", "Interplanetary Research Agency G"};
        FrontPage.Stance[] stances = {FrontPage.Stance.ENTHUSIASTIC, FrontPage.Stance.NEXT, FrontPage.Stance.DOWNPLAY,
            FrontPage.Stance.POLITE, FrontPage.Stance.POLITE, FrontPage.Stance.POLITE, null};
        for (int i = 0; i < names.length; i++) {
            crowd.add(new ReactionPrinter.Agency(names[i], "Alexandra Konstantinopoulou-Sim" + (char) ('a' + i), 1,
                java.util.Set.of("earth-orbit-satellite"), stances[i] == null ? java.util.Map.of()
                    : java.util.Map.of(space.id(), new FrontPage.Statement(space.id(), stances[i], quote))));
        }
        FrontPage crowded = ReactionPrinter.print(26, 7L, List.of(space), crowd, words, FrontPages.standard()).get(0);
        game.session().keepFrontPages(List.of(satellite, space, crowded));
        write(folder, "reactions-page-crowded.png", new FrontPageScreen(game, List.of(crowded), () -> { }, false),
            null);
        // More statements than the printer ever tells, to see the article run on to its continuation sheet.
        List<FrontPage.Reaction> many = new java.util.ArrayList<>(crowded.reactions());
        for (FrontPage.Reaction reaction : crowded.reactions()) {
            if (!words.shared(reaction.lineId())) {
                many.add(0, reaction);
            }
        }
        FrontPage overflowing = new FrontPage(crowded.id(), crowded.goalId(), crowded.gameTurn(), crowded.storyId(),
            crowded.weather(), crowded.foldedGoalIds(), crowded.achievers(), crowded.rivals(), crowded.kind(),
            crowded.reactsTo(), many);
        game.session().keepFrontPages(List.of(satellite, space, overflowing));
        write(folder, "reactions-page-overflow.png", new FrontPageScreen(game, List.of(overflowing), () -> { },
            false), null);
        write(folder, "reactions-page-overflow-2.png", new FrontPageScreen(game, List.of(overflowing), () -> { },
            false), FrontPageScreen::showContinuation);
    }

    private static ReactionPrinter.Agency agencyOf(String name, String director, FrontPage.Stance stance, String quote) {
        return new ReactionPrinter.Agency(name, director, 1, java.util.Set.of("earth-orbit-satellite"),
            stance == null ? java.util.Map.of() : java.util.Map.of("25-human-in-space",
                new FrontPage.Statement("25-human-in-space", stance, quote)));
    }

    /**
     * The hands-on decision (ui-spec.md component 26, type 11, 2026-10-11): the banner and its
     * confirmation, the campaign planning, a round's results, a fail waiting for an answer, the three
     * result panels, the banner afterwards and in progress, the report's card and the hub.
     */
    private static void handsOnShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 23, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot state = planningState(11, 25_000);
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(57.2, 3.0));
        SetSpecialist lead = specialist("s3", "Gleb Arkhipov", 41, 70, 52, 74, 20, 74, 30, 12);
        lead.setAssignmentState(AssignmentState.ASSIGNED);
        lead.setRdTargetComponentId("r7-vostok");
        lead.setRdSlot(0);
        state.getSetSpecialists().add(lead);
        startSession(game, state, info);
        PlayerSnapshot live = game.session().state();
        de.mkoehler.launchwindow.content.ContentCatalog catalog = game.session().catalog();
        de.mkoehler.launchwindow.content.Decisions decisions = de.mkoehler.launchwindow.content.Decisions.standard();
        java.util.function.LongFunction<de.mkoehler.launchwindow.model.PendingDecision> offer = seed -> {
            de.mkoehler.launchwindow.model.PendingDecision pending = de.mkoehler.launchwindow.model.PendingDecision
                .offered("big-test-stand", "r7-vostok", 22, seed);
            live.setPendingDecision(pending);
            return pending;
        };
        offer.apply(7L);
        write(folder, "handson-banner.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET), null);
        write(folder, "handson-confirm.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET),
            screen -> screen.openDecision("run", null));

        de.mkoehler.launchwindow.rules.RdDecisions.decide(live, catalog, decisions, "run", null, null);
        de.mkoehler.launchwindow.rules.RdDecisions.handsOn(live, catalog, decisions,
            de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.QUEUE, "make-firing");
        write(folder, "handson-planning.png", new de.mkoehler.launchwindow.client.screen.HandsOnScreen(game), null);

        // A seed whose round 1 leaves a fail waiting for an answer, and one with an exceptional result.
        for (long seed = 1; seed < 500; seed++) {
            offer.apply(seed);
            de.mkoehler.launchwindow.rules.RdDecisions.decide(live, catalog, decisions, "run", null, null);
            for (String command : List.of("make-firing", "sensor-sweep", "overtime")) {
                de.mkoehler.launchwindow.rules.RdDecisions.handsOn(live, catalog, decisions,
                    de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.QUEUE, command);
            }
            de.mkoehler.launchwindow.rules.RdDecisions.handsOn(live, catalog, decisions,
                de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.CONFIRM, null);
            if (live.getPendingDecision().handsOn().phase()
                    == de.mkoehler.launchwindow.model.HandsOnState.Phase.AWAITING_RESIST) {
                write(folder, "handson-fail.png", new de.mkoehler.launchwindow.client.screen.HandsOnScreen(game), null);
                break;
            }
        }
        for (long seed = 1; seed < 500; seed++) {
            offer.apply(seed);
            de.mkoehler.launchwindow.rules.RdDecisions.decide(live, catalog, decisions, "run", null, null);
            for (String command : List.of("make-firing", "sensor-sweep", "overtime")) {
                de.mkoehler.launchwindow.rules.RdDecisions.handsOn(live, catalog, decisions,
                    de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.QUEUE, command);
            }
            de.mkoehler.launchwindow.rules.RdDecisions.handsOn(live, catalog, decisions,
                de.mkoehler.launchwindow.rules.RdDecisions.HandsOnStep.CONFIRM, null);
            var puzzle = live.getPendingDecision().handsOn();
            if (puzzle.phase() == de.mkoehler.launchwindow.model.HandsOnState.Phase.PLANNING && puzzle.results()
                    .stream().anyMatch(slot -> slot.rolled() == de.mkoehler.launchwindow.model.HandsOnState.Roll
                    .EXCEPTIONAL)) {
                write(folder, "handson-round-done.png", new de.mkoehler.launchwindow.client.screen.HandsOnScreen(game),
                    screen -> screen.showRound(1));
                break;
            }
        }
        write(folder, "handson-in-progress.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET), null);
        TurnReport report = TurnReport.build(live, live, List.of(), 22, catalog);
        game.session().keepReport(report);
        write(folder, "handson-report.png", new TurnReportScreen(game, report), null);
        write(folder, "handson-hub.png", new SpaceComplexScreen(game), screen -> screen.hover(Hub.Place.VAB));

        // The three endings.
        String[][] endings = {{"BONUS", "bonus", "6", "8", "3", "12.3"}, {"SUCCESS", "success", "4", "5", "4", "6.2"},
            {"FAILURE", "failure", "2", "5", "4", "0"}};
        for (String[] ending : endings) {
            de.mkoehler.launchwindow.model.HandsOnState finished = new de.mkoehler.launchwindow.model.HandsOnState(
                java.util.Map.of("shift", 1, "firing", Integer.parseInt(ending[2]), "measurement",
                    Integer.parseInt(ending[3]), "part", 0), 4, List.of(), List.of(), Integer.parseInt(ending[4]),
                de.mkoehler.launchwindow.model.HandsOnState.Phase.FINISHED, 0, 9, 0.165, 0.15,
                de.mkoehler.launchwindow.model.HandsOnState.Result.valueOf(ending[0]));
            double gain = Double.parseDouble(ending[5]);
            live.setPendingDecision(new de.mkoehler.launchwindow.model.PendingDecision("big-test-stand", "r7-vostok",
                22, 7L, true, "run", ending[1], null, null, gain == 0 ? List.of()
                : List.of(new de.mkoehler.launchwindow.model.DecisionEffect(
                    de.mkoehler.launchwindow.model.DecisionEffect.Kind.RELIABILITY, gain, 0, null)), finished));
            write(folder, "handson-result-" + ending[1] + ".png",
                new de.mkoehler.launchwindow.client.screen.HandsOnScreen(game), null);
        }
        write(folder, "handson-banner-after.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET), null);
    }

    /**
     * Commercial contracts (ui-spec.md section 4.17 and component 27, 2026-10-11): the board with an
     * application out and with an active contract, the apply and move confirmations, the score's
     * tooltip, the empty board, Headquarters' card and the hub's dot, the markers on configuration
     * cards and a program's page, Assemble's contract box, and the report's card and news.
     */
    private static void contractShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 23, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        String line = "earth-orbit-satellites";
        de.mkoehler.launchwindow.model.ContractListing relay = new de.mkoehler.launchwindow.model.ContractListing(
            "c22-1", line, "sat-n-relay", "sat-s-relay", 23, 25);
        de.mkoehler.launchwindow.model.ContractListing weather = new de.mkoehler.launchwindow.model.ContractListing(
            "c22-2", line, "sat-n-weather", "sat-s-weather", 23, 25);
        de.mkoehler.launchwindow.model.ContractListing physics = new de.mkoehler.launchwindow.model.ContractListing(
            "c21-1", line, "sat-n-radiation", "sat-s-physics", 22, 23);
        de.mkoehler.launchwindow.model.ContractListing medicine = new de.mkoehler.launchwindow.model.ContractListing(
            "c22-3", "one-crew-capsules", "one-n-medicine", "one-s-medicine", 23, 25);

        PlayerSnapshot state = planningState(11, 25_000);
        state.getSuccessfulFlightCounts().put("sputnik-1-orbit", 3);
        state.setContractBoard(List.of(relay, weather, physics, medicine));
        state.getContractRecords().put(line, new de.mkoehler.launchwindow.model.ContractRecord(2, 0, 0));
        state.setContractApplication(new de.mkoehler.launchwindow.model.ContractApplication(weather.id(), "sputnik-1"));
        startSession(game, state, info);
        PlayerSnapshot live = game.session().state();
        write(folder, "contracts-applied.png", new de.mkoehler.launchwindow.client.screen.ContractsScreen(game), null);
        write(folder, "contracts-score.png", new de.mkoehler.launchwindow.client.screen.ContractsScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "Your score in"));
        write(folder, "contracts-move.png", new de.mkoehler.launchwindow.client.screen.ContractsScreen(game),
            screen -> screen.openApply(relay.id()));
        live.setContractApplication(null);
        write(folder, "contracts-confirm.png", new de.mkoehler.launchwindow.client.screen.ContractsScreen(game),
            screen -> screen.openApply(relay.id()));
        write(folder, "contracts-hq.png", new HeadquartersScreen(game), null);
        write(folder, "contracts-hub.png", new SpaceComplexScreen(game), screen -> screen.hover(Hub.Place.HEADQUARTERS));

        live.setActiveContract(new de.mkoehler.launchwindow.model.ActiveContract(physics.id(), line, "sat-s-physics",
            "sputnik-1", "sputnik-1-orbit", 1, 3_390, 22, 25, null));
        live.setContractBoard(List.of(relay, weather, medicine));
        write(folder, "contracts-active.png", new de.mkoehler.launchwindow.client.screen.ContractsScreen(game), null);
        write(folder, "contracts-hq-active.png", new HeadquartersScreen(game), null);
        write(folder, "contracts-assemble.png",
            new de.mkoehler.launchwindow.client.screen.AssembleMissionScreen(game, "sputnik-1-orbit"), null);
        write(folder, "contracts-configs-sputnik.png",
            new de.mkoehler.launchwindow.client.screen.MissionConfigsScreen(game, "sputnik-1"), null);
        write(folder, "contracts-configs-vostok.png",
            new de.mkoehler.launchwindow.client.screen.MissionConfigsScreen(game, "vostok"),
            screen -> showTooltip(screen.stage().getRoot(), "Unlock flight"));
        write(folder, "contracts-program.png", new ProgramDetailsScreen(game, "vostok",
            de.mkoehler.launchwindow.content.CelestialBody.EARTH), null);

        de.mkoehler.launchwindow.rules.ContractSettled awarded = new de.mkoehler.launchwindow.rules.ContractSettled(
            de.mkoehler.launchwindow.rules.ContractSettled.Kind.AWARDED, physics.id(), line, "sat-s-physics",
            "sputnik-1", "sputnik-1-orbit", 1, 3_390, 0, 0, null, 0, 0);
        TurnReport report = TurnReport.build(live, live, List.of(awarded,
            new de.mkoehler.launchwindow.rules.ContractNews(de.mkoehler.launchwindow.rules.ContractNews.Kind.AWARDED,
                physics.id(), line, "sat-s-physics", List.of("Baltic Star Agency", "Orbital Corp."), 0),
            new de.mkoehler.launchwindow.rules.ContractNews(de.mkoehler.launchwindow.rules.ContractNews.Kind.POSTED,
                null, null, null, List.of(), 2),
            new de.mkoehler.launchwindow.rules.ContractLineUnlocked(line, "sputnik-1", true)), 22,
            game.session().catalog());
        game.session().keepReport(report);
        write(folder, "contracts-report.png", new TurnReportScreen(game, report), null);
        de.mkoehler.launchwindow.rules.ContractSettled paid = new de.mkoehler.launchwindow.rules.ContractSettled(
            de.mkoehler.launchwindow.rules.ContractSettled.Kind.PAID, physics.id(), line, "sat-s-physics",
            "sputnik-1", "sputnik-1-orbit", 1, 3_390, 3_390, 0, "m7", 2_113, 3);
        TurnReport paidReport = TurnReport.build(live, live, List.of(paid), 22, game.session().catalog());
        write(folder, "contracts-report-paid.png", new TurnReportScreen(game, paidReport), null);

        PlayerSnapshot fresh = planningState(11, 25_000);
        startSession(game, fresh, info);
        write(folder, "contracts-empty.png", new de.mkoehler.launchwindow.client.screen.ContractsScreen(game), null);
    }

    /**
     * The standard pack, BASPM's whole tree (2026-10-08): the Moon's programs, the X-15's air drop
     * (no rocket picker) and its gated research flight, and Apollo's own lift easing the Saturn IB's
     * load.
     */
    private static void standardPackShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/standard", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("standard", hash, List.of(), 31, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot nasa = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 60_000, 2_140, 3_000, 31);
        nasa.setDirectorName("A. Reyes");
        nasa.getBuildingLevels().put(Building.VEHICLE_ASSEMBLY, 2);
        nasa.getOpenedProgramIds().addAll(List.of("x-15", "apollo-earth-orbit"));
        nasa.getMissionComponents().put("x-15-aircraft", new OwnedMissionComponent(64.0, 3.0));
        nasa.getMissionComponents().put("apollo-csm", new OwnedMissionComponent(58.0, 3.0));
        nasa.getMissionComponents().put("apollo-lunar-module", new OwnedMissionComponent(41.0, 3.0));
        nasa.getMissionComponents().put("saturn-1b", new OwnedMissionComponent(71.0, 3.0));
        nasa.getMissionComponents().put("titan-2", new OwnedMissionComponent(66.0, 3.0));
        startSession(game, nasa, info);
        write(folder, "standard-body-moon.png", new BodyProgramsScreen(game, CelestialBody.MOON), null);
        write(folder, "standard-x15-configs.png", new MissionConfigsScreen(game, "x-15"), null);
        write(folder, "standard-x15-gated.png", new MissionStepsScreen(game, "x-15-research-flight-1"), null);
        write(folder, "standard-x15-air-drop.png", new MissionPlanScreen(game, "x-15-flight-test",
            MissionPlanScreen.Tab.ROCKETS), null);
        write(folder, "standard-x15-assemble.png", new AssembleMissionScreen(game, "x-15-flight-test"), null);
        write(folder, "standard-apollo-rockets.png", new MissionPlanScreen(game, "apollo-orbital",
            MissionPlanScreen.Tab.ROCKETS), null);
        write(folder, "standard-vab-manage.png", new RocketProgramsScreen(game), null);

        // Goal boosts: Mercury's orbital flight has four, one of them (Pegasus) reached.
        nasa.getOpenedProgramIds().add("mercury");
        nasa.getMissionComponents().put("mercury-capsule", new OwnedMissionComponent(74.0, 3.0));
        nasa.getMissionComponents().put("atlas", new OwnedMissionComponent(69.0, 3.0));
        nasa.getAchievedGoals().put("micrometeoroid-satellite-flown", 20);
        startSession(game, nasa, info);
        write(folder, "standard-boosts-steps.png", new MissionStepsScreen(game, "mercury-orbital"), null);
        write(folder, "standard-boosts-plan.png", new MissionPlanScreen(game, "mercury-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);

        // The standard pack's concept chips (2026-10-09): the longest row, Apollo's landing, and
        // Apollo's program with its components' chips.
        write(folder, "standard-concepts-apollo-landing.png", new MissionStepsScreen(game, "apollo-landing"), null);
        write(folder, "standard-concepts-apollo.png", new ProgramDetailsScreen(game, "apollo", CelestialBody.MOON),
            null);

        // Looking ahead (2026-10-10): where a locked program's goal is earned, on its card and its page.
        write(folder, "outlook-locked-goal.png", new ProgramDetailsScreen(game, "pioneer-4", CelestialBody.MOON), null);
        write(folder, "outlook-locked-card.png", new BodyProgramsScreen(game, CelestialBody.MOON),
            screen -> showTooltip(screen.stage().getRoot(), "Where to earn them"));
    }

    /**
     * R&D decisions (ui-spec.md component 26, 2026-10-10): one banner per type on its board, the
     * confirmation with and without a picker, a good and a bad outcome, the turn report's card and
     * strip cell, and the hub's decision dot. Each decision is put on the state directly, not rolled.
     */
    private static void decisionShots(File folder, LaunchWindowGame game) {
        PlayerSnapshot live = decisionSession(game);

        String[][] banners = {
            {"static-firing", "r7-vostok", "decision-resource.png"},
            {"balloon-tanks-pad", "r7-vostok", "decision-knowledge.png"},
            {"night-shift", "vostok-capsule", "decision-ambiguous.png"},
            {"who-signs-vibration", "vostok-capsule", "decision-oversee.png"},
            {"freeze-design", "vostok-capsule", "decision-tradeoff.png"},
            {"battery-in-vacuum", "vostok-capsule", "decision-diagnosis.png"},
            {"altitude-chamber", "vostok-capsule", "decision-opportunity.png"},
            {"pair-up", "vostok-capsule", "decision-pairing.png"},
            {"rival-insulation", "vostok-capsule", "decision-intel.png"},
            {"long-soak", "vostok-capsule", "decision-followup.png"}};
        decisionBanners(folder, game, live, banners);
        // The confirmations: a cost, a picker for who goes, two pickers for a pair.
        live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered("static-firing", "r7-vostok",
            22, 7L));
        write(folder, "decision-confirm.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET),
            screen -> screen.openDecision("full", null));
        live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered("altitude-chamber",
            "vostok-capsule", 22, 7L));
        write(folder, "decision-confirm-seconded.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD),
            screen -> screen.openDecision("accept", null));
        live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered("pair-up", "vostok-capsule",
            22, 7L));
        write(folder, "decision-confirm-pair.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD),
            screen -> screen.openDecision("pair", null));
        decisionOutcomeShots(folder, game, live);
    }

    /**
     * Every R&amp;D decision's banner, alone with {@code -Dshots=decisions} (the pools grown 2026-10-25):
     * one shot each on a rocket or a payload as it fits ({@code pool-<id>.png}), and each hands-on
     * campaign's planning screen ({@code pool-<id>-planning.png}), to read the whole pool for fit.
     */
    private static void decisionPoolShots(File folder, LaunchWindowGame game) {
        PlayerSnapshot live = decisionSession(game);
        de.mkoehler.launchwindow.content.Decisions decisions = de.mkoehler.launchwindow.content.Decisions.standard();
        List<String[]> banners = new java.util.ArrayList<>();
        for (de.mkoehler.launchwindow.content.Decisions.Decision decision : decisions.decisions()) {
            String component = decision.fits() == de.mkoehler.launchwindow.content.Decisions.Fits.ROCKET
                ? "r7-vostok" : "vostok-capsule";
            banners.add(new String[] {decision.id(), component, "pool-" + decision.id() + ".png"});
        }
        decisionBanners(folder, game, live, banners.toArray(new String[0][]));
        for (de.mkoehler.launchwindow.content.Decisions.Decision decision : decisions.decisions()) {
            if (decision.handsOn() == null) {
                continue;
            }
            String component = decision.fits() == de.mkoehler.launchwindow.content.Decisions.Fits.ROCKET
                ? "r7-vostok" : "vostok-capsule";
            live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered(decision.id(), component,
                22, 7L));
            de.mkoehler.launchwindow.rules.RdDecisions.decide(live, game.session().catalog(), decisions, "run", null,
                null);
            write(folder, "pool-" + decision.id() + "-planning.png",
                new de.mkoehler.launchwindow.client.screen.HandsOnScreen(game), null);
        }
    }

    /** One banner per row (decision, component, file) on its component's board. */
    private static void decisionBanners(File folder, LaunchWindowGame game, PlayerSnapshot live, String[][] banners) {
        for (String[] banner : banners) {
            live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered(banner[0], banner[1], 22,
                7L));
            RndBoardScreen.Board board = banner[1].equals("r7-vostok") ? RndBoardScreen.Board.ROCKET
                : RndBoardScreen.Board.PAYLOAD;
            write(folder, banner[2], new RndBoardScreen(game, board), null);
        }
    }

    /**
     * The decisions' session: a Soviet agency in 1960 with the R-7 and Vostok under R&amp;D, a team on
     * each, idle engineers to oversee and pair, one in training, and an Intelligence Office at level 2.
     */
    private static PlayerSnapshot decisionSession(LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 23, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot state = planningState(11, 25_000);
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(48.5, 8.7));
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(57.0, 3.0));
        SetSpecialist second = state.getSetSpecialists().get(1);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("vostok-capsule");
        second.setRdSlot(1);
        SetSpecialist rocketeer = specialist("s3", "Gleb Arkhipov", 41, 70, 52, 74, 20, 77, 30, 12);
        rocketeer.setAssignmentState(AssignmentState.ASSIGNED);
        rocketeer.setRdTargetComponentId("r7-vostok");
        rocketeer.setRdSlot(0);
        state.getSetSpecialists().add(rocketeer);
        state.getSetSpecialists().add(specialist("s4", "Irina Lebedeva", 36, 70, 48, 55, 61, 48, 71, 30));
        state.getSetSpecialists().add(specialist("s5", "Pyotr Kuznetsov", 47, 70, 50, 62, 40, 58, 52, 22));
        SetSpecialist training = specialist("s6", "Anna Sokolova", 33, 70, 46, 44, 52, 39, 66, 28);
        training.setAssignmentState(AssignmentState.IN_TRAINING);
        training.setTrainingKind(TrainingKind.ADVANCED);
        training.setTrainingSeasonsTotal(3);
        training.setTrainingSeasonsCompleted(1);
        state.getSetSpecialists().add(training);
        state.getIntelligence().setLevel(2);
        startSession(game, state, info);
        return game.session().state();
    }

    /** The decided banners, good and bad, then where the player meets a decision: the report and the hub. */
    private static void decisionOutcomeShots(File folder, LaunchWindowGame game, PlayerSnapshot live) {
        // The outcomes: the test firing's clean run and its rough one, by the seed's roll.
        de.mkoehler.launchwindow.content.Decisions decisions = de.mkoehler.launchwindow.content.Decisions.standard();
        for (boolean good : new boolean[] {true, false}) {
            long seed = 1;
            while ((new java.util.SplittableRandom(seed).nextDouble() < 0.7) != good) {
                seed++;
            }
            live.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(57.0, 3.0));
            live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered("static-firing",
                "r7-vostok", 22, seed));
            de.mkoehler.launchwindow.rules.RdDecisions.decide(live, game.session().catalog(), decisions, "full", null,
                null);
            write(folder, good ? "decision-decided-good.png" : "decision-decided-bad.png",
                new RndBoardScreen(game, RndBoardScreen.Board.ROCKET), null);
        }

        // Where the player meets it: the report's card and strip cell, the hub's dot.
        live.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(57.0, 3.0));
        live.setPendingDecision(de.mkoehler.launchwindow.model.PendingDecision.offered("static-firing", "r7-vostok",
            22, 7L));
        TurnReport report = TurnReport.build(live, live, List.of(), 22, game.session().catalog());
        game.session().keepReport(report);
        write(folder, "decision-report.png", new TurnReportScreen(game, report), null);
        write(folder, "decision-hub.png", new SpaceComplexScreen(game), screen -> screen.hover(Hub.Place.VAB));
    }

    /**
     * Looking ahead (the user's request, 2026-10-10): an R&D board's milestones and their tooltip, a
     * rocket engineer who retires before the cap, Vostok's earliest launch and its missions' checks
     * while cosmonauts finish basic training, the roster's ready-by and retirement notes, and the
     * program cards' staffing.
     */
    private static void outlookShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 23, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot state = planningState(11, 25_000);
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(48.5, 8.7));
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(57.0, 3.0));
        SetSpecialist second = state.getSetSpecialists().get(1);
        second.setAssignmentState(AssignmentState.ASSIGNED);
        second.setRdTargetComponentId("vostok-capsule");
        second.setRdSlot(1);
        SetSpecialist veteranEngineer = specialist("s3", "Gleb Arkhipov", 63, 70, 52, 40, 20, 77, 30, 12);
        veteranEngineer.setAgingProgressSeasons(1);
        veteranEngineer.setAssignmentState(AssignmentState.ASSIGNED);
        veteranEngineer.setRdTargetComponentId("r7-vostok");
        veteranEngineer.setRdSlot(0);
        state.getSetSpecialists().add(veteranEngineer);
        // Four cosmonauts in basic training, finishing at different times; one near retirement.
        for (int i = 0; i < 4; i++) {
            Astronaut recruit = state.getAstronauts().get(i);
            recruit.setAssignmentState(AssignmentState.IN_TRAINING);
            recruit.setTrainingKind(TrainingKind.BASIC);
            recruit.setTrainingSeasonsTotal(4);
            recruit.setTrainingSeasonsCompleted(1 + i % 3);
        }
        Astronaut veteran = state.getAstronauts().get(5);
        veteran.setAge(49);
        veteran.setAgingProgressSeasons(2);
        startSession(game, state, info);
        write(folder, "outlook-rnd.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD),
            screen -> showTooltip(screen.stage().getRoot(), "Seasons to each mark"));
        write(folder, "outlook-rocket-rnd.png", new RndBoardScreen(game, RndBoardScreen.Board.ROCKET), null);
        write(folder, "outlook-program.png", new ProgramDetailsScreen(game, "vostok", CelestialBody.EARTH), null);
        write(folder, "outlook-configs.png", new MissionConfigsScreen(game, "vostok"),
            screen -> showTooltip(screen.stage().getRoot(), "Ready to fly?"));
        write(folder, "outlook-ac.png", new AstronautComplexScreen(game), null);
        write(folder, "outlook-body-staffing.png", new BodyProgramsScreen(game, CelestialBody.EARTH), screen -> {
            screen.select("one-crew-capsules");
            screen.stage().draw();
            showTooltip(screen.stage().getRoot(), "People its missions need");
        });
        write(folder, "outlook-pao.png", new PublicAffairsScreen(game), null);
        // Ambitions at stake (2026-10-10): every free controller wants an orbital console, so the one the
        // draft leaves out is passed over; the picker marks who would get their wish.
        for (MissionControlOfficer officer : game.session().state().getMissionControlOfficers()) {
            officer.setAmbition(new de.mkoehler.launchwindow.model.Ambition(
                de.mkoehler.launchwindow.model.Ambition.Kind.CONSOLES_ORBITAL, "", 6, 12, 1));
        }
        write(folder, "assemble-passed-over.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        write(folder, "assemble-wants-this.png", new AssembleMissionScreen(game, "vostok-orbital"),
            screen -> screen.openPicker(false, 0));
        // Idle people (2026-10-10): red dots on the markers, and the count under the hovered name.
        write(folder, "outlook-hub-idle.png", new SpaceComplexScreen(game),
            screen -> screen.hover(Hub.Place.MISSION_CONTROL));
        // The astronauts' roster sorted by a skill (2026-10-10).
        write(folder, "ac-sorted.png", new AstronautComplexScreen(game),
            screen -> screen.sortBy(de.mkoehler.launchwindow.model.AstronautSkill.PILOTING));
    }

    /**
     * The advisor (ui-spec.md component 24, 2026-10-08): a new agency's card, one part-way on the
     * NASA art (the corner clear of its markers too), the waiting card once everything is done
     * before the first turn resolves, help opened from an item, and the hub once retired.
     */
    private static void advisorShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, List.of(), 1, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        java.util.Set<String> marks = new java.util.HashSet<>(java.util.Set.of("idleTipDone"));
        game.useAdvisorMarksInMemory(marks);
        PlayerSnapshot fresh = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 25_000, 250, 3_000, 1);
        fresh.setDirectorName("A. Reyes");
        startSession(game, fresh, info);
        write(folder, "advisor-new.png", new SpaceComplexScreen(game), null);

        marks.add("helpOpened");
        PlayerSnapshot nasa = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 23_350, 250, 3_000, 1);
        nasa.setDirectorName("A. Reyes");
        SetSpecialist engineer = new SetSpecialist("e1", "R. Hale", 34, 70, 50);
        nasa.getSetSpecialists().add(engineer);
        nasa.getOpenedProgramIds().add("explorer-1");
        startSession(game, nasa, info);
        write(folder, "advisor-nasa-progress.png", new SpaceComplexScreen(game), null);
        // Minimized to its head, which counts what's left; every later shot has it open again.
        game.setAdvisorMinimized(true);
        write(folder, "advisor-minimized.png", new SpaceComplexScreen(game), null);
        game.setAdvisorMinimized(false);
        write(folder, "advisor-rules.png", new SpaceComplexScreen(game), screen -> screen.openHelp(
            SpaceComplexScreen.advisorTopics(de.mkoehler.launchwindow.client.view.Advisor.ITEMS.get(3))));

        nasa.getMissionComponents().put("explorer-1-satellite", new OwnedMissionComponent(41.7, 3));
        nasa.getMissionComponents().put("jupiter-c", new OwnedMissionComponent(22, 3));
        engineer.setRdTargetComponentId("explorer-1-satellite");
        nasa.getMissionControlOfficers().add(new MissionControlOfficer("c1", "J. Ortiz", 31, 70, 50));
        startSession(game, nasa, info);
        // Everything set up but the hardware: the mission waits for reliability.
        write(folder, "advisor-reliability.png", new SpaceComplexScreen(game), null);
        nasa.getMissionComponents().get("jupiter-c").setCurrentReliability(63);
        nasa.getMissionComponents().get("explorer-1-satellite").setCurrentReliability(61);
        startSession(game, nasa, info);
        write(folder, "advisor-ready.png", new SpaceComplexScreen(game), null);
        nasa.setLastFlightSeasonTurn(1);
        // The capsule open and crewed, so only the report is left to wait for (the road ahead, 2026-10-26).
        nasa.getOpenedProgramIds().add("mercury");
        nasa.getAstronauts().add(new Astronaut("a1", "D. Slayton", 35, 70, 50));
        startSession(game, nasa, info);
        write(folder, "advisor-waiting.png", new SpaceComplexScreen(game), null);

        game.retireAdvisor();
        write(folder, "advisor-retired.png", new SpaceComplexScreen(game), null);
    }

    /**
     * Help (ui-spec.md component 23, 2026-10-08): opened from the hub (every topic), from the SET
     * Centre (filtered, with the chip), a topic with links and concept chips, a search, a search
     * that finds nothing, and the header's help button with its tooltip.
     */
    private static void helpShots(File folder, LaunchWindowGame game) {
        write(folder, "help-hub.png", new SpaceComplexScreen(game), screen -> screen.openHelp());
        write(folder, "help-set-centre.png", new SetCentreScreen(game), screen -> screen.openHelp());
        write(folder, "help-rnd-links.png", new SetCentreScreen(game),
            screen -> screen.openHelp().select("rnd"));
        write(folder, "help-poaching.png", new SetCentreScreen(game),
            screen -> screen.openHelp().select("poaching"));
        write(folder, "help-search.png", new SetCentreScreen(game), screen -> screen.openHelp().search("loyalty"));
        write(folder, "help-list-end.png", new SpaceComplexScreen(game), screen -> {
            var help = screen.openHelp();
            help.select("intelligence");
            screen.stage().act(0f);
            help.scrollListToEnd();
        });
        write(folder, "help-traits.png", new SetCentreScreen(game), screen -> screen.openHelp().select("traits"));
        write(folder, "help-buildings.png", new SpaceComplexScreen(game), screen -> screen.openHelp().select("buildings"));
        write(folder, "help-mission-steps.png", new SpaceComplexScreen(game), screen -> screen.openHelp().select("mission-steps"));
        write(folder, "help-search-none.png", new SetCentreScreen(game),
            screen -> screen.openHelp().search("sabotage of the moon"));
        write(folder, "help-button-tooltip.png", new SetCentreScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "Help (F1)"));
        // The keys: F1 alone opens help (a chrome screen and the hub); Esc closes it while the
        // search field has the keyboard, leaving the plain screen.
        write(folder, "help-f1-set-centre.png", new SetCentreScreen(game),
            screen -> screen.stage().keyDown(com.badlogic.gdx.Input.Keys.F1));
        write(folder, "help-f1-hub.png", new SpaceComplexScreen(game),
            screen -> screen.stage().keyDown(com.badlogic.gdx.Input.Keys.F1));
        write(folder, "help-esc-closed.png", new SetCentreScreen(game), screen -> {
            screen.openHelp().search("loyalty");
            screen.stage().keyDown(com.badlogic.gdx.Input.Keys.ESCAPE);
        });
    }

    /**
     * The Poaching canvas (2026-10-08): a rival's folder with an offer to make, the reasons there
     * can't be one and an offer queued, the offer dialog (with room, and with the roster full), the
     * queued offer among the operations, the SET Centre's Retention strip and roster marks, the person
     * page's Retention row and someone who joined from a rival, and the turn report's offer lines.
     */
    private static void poachingShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, DECLARED, 31, true,
            6 * 3_600_000L + 12 * 60_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot state = setCentreState();
        state.getIntelligence().setLevel(3);
        state.getIntelligence().setTargetLevel(3);
        state.getIntelligence().setActionsUsed(2);
        PlayerSnapshot pool = new PlayerSnapshot("Sever Design Bureau", Archetype.SOVIET, 0, 0, 0, 1);
        HiringPool.refreshCandidates(pool, new java.util.SplittableRandom(11));
        de.mkoehler.launchwindow.model.RivalFolder sever = state.getIntelligence().folder("kSever",
            "Sever Design Bureau");
        sever.learnLevel(2, 30, de.mkoehler.launchwindow.model.RivalFolder.FROM_DOSSIER);
        Personnel engineer = pool.getSetSpecialistCandidates().get(2);
        Personnel astronaut = pool.getAstronautCandidates().get(4);
        Personnel newcomer = pool.getSetSpecialistCandidates().get(0);
        Personnel controller = pool.getMissionControlCandidates().get(1);
        List<de.mkoehler.launchwindow.model.IntelFact> engineerSkills = List.of(fact("SKILL", "CREWED_SPACECRAFT", 66, 76,
            0), fact("SKILL", "ROCKETS", 52, 62, 0), fact("SKILL", "SPACE_PROBES", 40, 50, 0));
        sever.getPersonnelFiles().add(new de.mkoehler.launchwindow.model.PersonnelFile(controller.getId(),
            controller.getName(), controller.getPortraitId(), "MISSION_CONTROL", controller.getAge(), 30, List.of(
                fact("SKILL", "TRAJECTORY_AND_GUIDANCE", 64, 74, 0), fact("SKILL", "PROPULSION", 58, 68, 0),
                fact("SKILL", "MISSION_OPERATIONS", 51, 61, 0)), de.mkoehler.launchwindow.model.Trait.RESTLESS,
            "Middling", null, false, 36, 9, null));
        sever.getPersonnelFiles().add(new de.mkoehler.launchwindow.model.PersonnelFile(astronaut.getId(),
            astronaut.getName(), astronaut.getPortraitId(), "ASTRONAUT", astronaut.getAge(), 30, List.of(
                fact("SKILL", "PILOTING", 74, 84, 0)), de.mkoehler.launchwindow.model.Trait.GLORY_SEEKER, "High", null,
            false, 40, 12, null));
        sever.getPersonnelFiles().add(new de.mkoehler.launchwindow.model.PersonnelFile(newcomer.getId(),
            newcomer.getName(), newcomer.getPortraitId(), "SET", newcomer.getAge(), 30, engineerSkills,
            de.mkoehler.launchwindow.model.Trait.QUICK_STUDY, "Middling", null, false, 30, 1, null));
        sever.getPersonnelFiles().add(new de.mkoehler.launchwindow.model.PersonnelFile(engineer.getId(),
            engineer.getName(), engineer.getPortraitId(), "SET", engineer.getAge(), 30, engineerSkills,
            de.mkoehler.launchwindow.model.Trait.STEADY, "Low", new de.mkoehler.launchwindow.model.Ambition(
                de.mkoehler.launchwindow.model.Ambition.Kind.TOP_ENGINEER, "", 9, 12, 26), true, 38, 14, null));
        startSession(game, state, info);
        game.session().keepRivals(List.of(RivalSummary.of("Sever Design Bureau", "Tamara Abenova", Archetype.SOVIET,
            false, Map.of("earth-orbit-satellite", 8), "kSever")));
        de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab folders =
            de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.FOLDERS;
        write(folder, "poach-folders-free.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            folders), screen -> screen.openFolder("kSever"));
        de.mkoehler.launchwindow.model.PersonnelFile controllerFile = sever.getPersonnelFiles().getFirst();
        de.mkoehler.launchwindow.model.PersonnelFile engineerFile = sever.getPersonnelFiles().getLast();
        write(folder, "poach-offer.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            folders), screen -> {
                screen.openFolder("kSever");
                screen.openOffer("kSever", controllerFile);
            });
        // A full SET Centre: the offer at +100 % would lapse.
        int cap = de.mkoehler.launchwindow.rules.BuildingUpgrades.capacity(game.session().state(), Building.SET_CENTER,
            game.session().catalog().buildings());
        for (int i = game.session().state().getSetSpecialists().size(); i < cap; i++) {
            game.session().state().getSetSpecialists().add(specialist("f" + i, "Filler " + i, 30, 70, 30, 40, 40,
                40, 40, 40));
        }
        write(folder, "poach-offer-full.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            folders), screen -> {
                screen.openFolder("kSever");
                screen.pickRaise(100);
                screen.openOffer("kSever", engineerFile);
            });
        game.session().state().getSetSpecialists().removeIf(person -> person.getId().startsWith("f"));
        game.session().state().getIntelligence().getQueued().add(new de.mkoehler.launchwindow.model.IntelOperation(
            "op8", de.mkoehler.launchwindow.model.IntelOperation.Kind.ASSESSMENT, "kSever",
            de.mkoehler.launchwindow.model.IntelArea.RND, null, null));
        game.session().state().getIntelligence().getQueued().add(new de.mkoehler.launchwindow.model.IntelOperation(
            "op9", de.mkoehler.launchwindow.model.IntelOperation.Kind.OFFER, "kSever", null, "SET", engineer.getId(),
            50));
        write(folder, "poach-folders-queued.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(
            game, folders), screen -> screen.openFolder("kSever"));
        write(folder, "poach-operations.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.OPERATIONS), null);

        // Keeping people: one kept at any price, one let go, a person page on each order, a newcomer.
        PlayerSnapshot own = game.session().state();
        own.getSetSpecialists().get(0).setRetention(de.mkoehler.launchwindow.model.Retention.KEEP);
        own.getSetSpecialists().get(3).setRetention(de.mkoehler.launchwindow.model.Retention.LET_GO);
        write(folder, "poach-set-retention.png", new SetCentreScreen(game), null);
        SetSpecialist kept = own.getSetSpecialists().get(0);
        kept.setLoyalty(69);
        kept.getCareer().add(career(26, "HIRED", "", "calm", "", 0, 0));
        kept.getCareer().add(career(29, "OFFER", "Sever Design Bureau", "", "TURNED_DOWN", 52, 25));
        kept.getCareer().add(career(30, "OFFER", "Sever Design Bureau", "52", "MATCHED", 78, 50));
        write(folder, "poach-person-keep.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game, kept,
            () -> new SetCentreScreen(game)), null);
        SetSpecialist plain = own.getSetSpecialists().get(5);
        write(folder, "poach-person-default.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game, plain,
            () -> new SetCentreScreen(game)), null);
        SetSpecialist joined = own.getSetSpecialists().get(4);
        joined.setLoyalty(50);
        joined.setTraits(List.of(new de.mkoehler.launchwindow.model.TraitState(
            de.mkoehler.launchwindow.model.Trait.PERFECTIONIST, 2, true), new de.mkoehler.launchwindow.model.TraitState(
            de.mkoehler.launchwindow.model.Trait.MENTOR, 3, false)));
        joined.getCareer().add(career(12, "HIRED", "", "exacting", "", 0, 0));
        joined.getCareer().add(career(18, "TRAIT", "PERFECTIONIST", "", "", 0, 0));
        joined.getCareer().add(career(25, "RD_STINT", "vostok-capsule", "", "", 6.8, 4));
        joined.getCareer().add(career(30, "JOINED", "Sever Design Bureau", "38", "", 57, 50));
        write(folder, "poach-person-joined.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game, joined,
            () -> new SetCentreScreen(game)), null);

        TurnReport report = new TurnReport(TurnReport.VERSION, 30, 4_310, 4_370, 21_000, 21_512, List.of(), List.of(),
            List.of(
                new de.mkoehler.launchwindow.rules.OfferSettled(false, "kSever", "Sever Design Bureau", kept.getId(),
                    kept.getName(), "SET", 50, de.mkoehler.launchwindow.rules.OfferResult.Outcome.MATCHED, 52, 78, 0,
                    "KEEP", -1),
                new de.mkoehler.launchwindow.rules.OfferSettled(false, "kMeridian", "Meridian Rocket Society", "p40",
                    "Rein Saar", "SET", 100, de.mkoehler.launchwindow.rules.OfferResult.Outcome.JOINED, 44, 88, 0,
                    "LET_GO", -1),
                new de.mkoehler.launchwindow.rules.OfferSettled(true, "kSever", "Sever Design Bureau", joined.getId(),
                    joined.getName(), "SET", 50, de.mkoehler.launchwindow.rules.OfferResult.Outcome.JOINED, 38, 57, 57,
                    "", -1),
                new de.mkoehler.launchwindow.rules.OfferSettled(true, "kSever", "Sever Design Bureau", "p9",
                    "Lucille Banks", "MISSION_CONTROL", 100,
                    de.mkoehler.launchwindow.rules.OfferResult.Outcome.LAPSED_NO_ROOM, 42, 84, 0, "", -1)));
        game.session().keepReport(report);
        write(folder, "poach-report.png", new TurnReportScreen(game, report), null);
    }

    /**
     * People phase 4 (2026-10-07): a person page with an ambition, loyalty and a career record, one
     * disappointed, the roster's ambition lines, the turn report's ambition lines, and the
     * memorial's person page.
     */
    private static void careerShots(File folder, LaunchWindowGame game, GameInfoResponse info) {
        PlayerSnapshot state = peopleState();
        List<SetSpecialist> set = state.getSetSpecialists();
        SetSpecialist veteran = set.get(0);
        veteran.setLoyalty(72);
        veteran.setTenureSeasons(12);
        veteran.setAmbition(new de.mkoehler.launchwindow.model.Ambition(
            de.mkoehler.launchwindow.model.Ambition.Kind.COMPONENT_CAP, "vostok-capsule", 4, 6, 10));
        veteran.setStintComponentId("vostok-capsule");
        veteran.setStintSeasons(7);
        veteran.setStintGain(16.4);
        veteran.setCareer(List.of(
            career(2, "HIRED", "", "driven", "", 0, 0),
            career(4, "TRAINING", "CREWED_SPACECRAFT", "ADVANCED", "", 4.5, 0),
            career(5, "TRAIT", "WORKAHOLIC", "", "", 0, 0),
            career(6, "RD_STINT", "sputnik-1-satellite", "", "", 11.0, 5),
            career(7, "TRAIT", "MENTOR", "", "", 0, 0),
            career(9, "RELATIONSHIP", set.get(4).getName(), "rd:vostok-capsule", "", 1, 7),
            career(10, "AMBITION", "COMPONENT_CAP", "vostok-capsule", "SAID", 0, 6),
            career(12, "RELATIONSHIP", set.get(1).getName(), "rd:vostok-capsule", "", -1, 7)));
        SetSpecialist sulking = set.get(4);
        sulking.setLoyalty(38);
        sulking.setDisappointedSeasons(3);
        sulking.setCareer(List.of(career(3, "HIRED", "", "calm", "", 0, 0),
            career(5, "AMBITION", "TOP_ENGINEER", "", "SAID", 0, 6),
            career(11, "AMBITION", "TOP_ENGINEER", "", "RAN_OUT", 0, 0)));
        de.mkoehler.launchwindow.model.Astronaut lost = new de.mkoehler.launchwindow.model.Astronaut("p90",
            "Hector Santiago", 36, 70, 50);
        lost.setPortraitId(set.get(2).getPortraitId());
        for (de.mkoehler.launchwindow.model.AstronautSkill skill : de.mkoehler.launchwindow.model.AstronautSkill.values()) {
            lost.setSkill(skill, 48 + 7 * skill.ordinal());
        }
        lost.setTraits(List.of(new de.mkoehler.launchwindow.model.TraitState(
            de.mkoehler.launchwindow.model.Trait.GLORY_SEEKER, 0, true)));
        lost.setCareer(List.of(career(2, "HIRED", "", "sociable", "", 0, 0),
            career(8, "FLIGHT", "vostok-suborbital", "CREW", "", 0, 0),
            career(9, "FIRST", "human-in-space", "vostok-suborbital", "", 0, 0)));
        state.getMemorial().add(de.mkoehler.launchwindow.rules.Careers.memorial(lost, 12,
            de.mkoehler.launchwindow.model.MemorialEntry.Cause.MISSION, "vostok-orbital", "retrofire")
            .withCareer(career(12, "FLIGHT", "vostok-orbital", "CREW", "retrofire", 0, 0)));
        startSession(game, state, info);
        write(folder, "career-person.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game, veteran,
            () -> new SetCentreScreen(game)), null);
        write(folder, "career-person-disappointed.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game,
            sulking, () -> new SetCentreScreen(game)), null);
        write(folder, "career-roster.png", new SetCentreScreen(game), null);
        write(folder, "career-memorial-plaques.png", new MuseumScreen(game, MuseumScreen.Tab.MEMORIAL), null);
        write(folder, "career-memorial-page.png", new de.mkoehler.launchwindow.client.screen.MemorialPersonScreen(game,
            state.getMemorial().size() - 1, () -> new MuseumScreen(game, MuseumScreen.Tab.MEMORIAL)), null);
        TurnReport report = new TurnReport(TurnReport.VERSION, 13, 2_100, 2_140, 17_000, 18_420, List.of(), List.of(),
            List.of(
                new de.mkoehler.launchwindow.rules.AmbitionSettled(PersonnelGroup.SET, set.get(3).getId(),
                    set.get(3).getName(), de.mkoehler.launchwindow.model.Ambition.Kind.FIRST_HARDWARE, "",
                    de.mkoehler.launchwindow.rules.AmbitionSettled.Outcome.SAID, 8, 0, 0),
                new de.mkoehler.launchwindow.rules.AmbitionSettled(PersonnelGroup.SET, set.get(2).getId(),
                    set.get(2).getName(), de.mkoehler.launchwindow.model.Ambition.Kind.COMPONENT_CAP,
                    "sputnik-1-satellite", de.mkoehler.launchwindow.rules.AmbitionSettled.Outcome.FULFILLED, 2, 5, 15),
                new de.mkoehler.launchwindow.rules.AmbitionSettled(PersonnelGroup.SET, sulking.getId(),
                    sulking.getName(), de.mkoehler.launchwindow.model.Ambition.Kind.TOP_ENGINEER, "",
                    de.mkoehler.launchwindow.rules.AmbitionSettled.Outcome.RAN_OUT, 0, -10, -15)));
        game.session().keepReport(report);
        write(folder, "career-report.png", new TurnReportScreen(game, report), null);
    }

    private static de.mkoehler.launchwindow.model.CareerEntry career(int turn, String kind, String ref, String note,
            String outcome, double value, int count) {
        return new de.mkoehler.launchwindow.model.CareerEntry(turn,
            de.mkoehler.launchwindow.model.CareerEntry.Kind.valueOf(kind), ref, note, outcome, value, count);
    }

    /**
     * The Intelligence Office canvas (2026-10-07): the overview at level 2 climbing to 3 and at level
     * 0, an assessment being picked, a rival's folder and an empty one, the hire screen's background
     * check, the hub's new building, and the turn report's Intelligence section.
     */
    private static void intelligenceShots(File folder, LaunchWindowGame game) {
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, DECLARED, 31, true,
            6 * 3_600_000L + 12 * 60_000L, 0L, null, List.of(), List.of(), 0);
        PlayerSnapshot state = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 21_640, 4_310, 3_000, 31);
        state.setDirectorName("A. Reyes");
        de.mkoehler.launchwindow.model.IntelligenceState intel = state.getIntelligence();
        intel.setLevel(2);
        intel.setTargetLevel(3);
        intel.setActionsUsed(1);
        intel.getQueued().add(new de.mkoehler.launchwindow.model.IntelOperation("op7",
            de.mkoehler.launchwindow.model.IntelOperation.Kind.ASSESSMENT, "kSever",
            de.mkoehler.launchwindow.model.IntelArea.RND, null, null));
        intel.setNextOperationId(8);

        PlayerSnapshot pool = new PlayerSnapshot("Sever Design Bureau", Archetype.SOVIET, 0, 0, 0, 1);
        HiringPool.refreshCandidates(pool, new java.util.SplittableRandom(11));
        de.mkoehler.launchwindow.model.RivalFolder sever = intel.folder("kSever", "Sever Design Bureau");
        sever.learnLevel(1, 30, de.mkoehler.launchwindow.model.RivalFolder.FROM_DOSSIER);
        sever.getDossiers().put(de.mkoehler.launchwindow.model.IntelArea.RND, new de.mkoehler.launchwindow.model.Dossier(
            de.mkoehler.launchwindow.model.IntelArea.RND, 29, List.of(
                fact("COMPONENT_RELIABILITY", "r7-sputnik", 72, 92, 0),
                fact("COMPONENT_RELIABILITY", "r7-vostok", 70, 90, 0),
                fact("COMPONENT_RELIABILITY", "sputnik-1-satellite", 80, 92, 0),
                fact("COMPONENT_RELIABILITY", "sputnik-2-satellite", 76, 94, 0),
                fact("COMPONENT_RELIABILITY", "vostok-capsule", 64, 84, 0)), 3));
        sever.getDossiers().put(de.mkoehler.launchwindow.model.IntelArea.PLANS, new de.mkoehler.launchwindow.model.Dossier(
            de.mkoehler.launchwindow.model.IntelArea.PLANS, 30, List.of(
                fact("OPEN_PROGRAM", "sputnik-2", 0, 0, 0), fact("OPEN_PROGRAM", "vostok", 0, 0, 0),
                fact("IN_FLIGHT", "vostok-orbital", 0, 0, 0),
                fact("RD_FOCUS", "vostok-capsule", 0, 0, 3), fact("RD_FOCUS", "r7-vostok", 0, 0, 2),
                fact("SHELF", "sputnik-2-satellite", 0, 0, 0), fact("INTEL_LEVEL", "", 0, 0, 1)), 1));
        sever.getDossiers().put(de.mkoehler.launchwindow.model.IntelArea.PERSONNEL,
            new de.mkoehler.launchwindow.model.Dossier(de.mkoehler.launchwindow.model.IntelArea.PERSONNEL, 30, List.of(
                fact("HEADCOUNT", "ASTRONAUT", 0, 0, 0), fact("HEADCOUNT", "SET", 0, 0, 7),
                fact("AVERAGE_SKILL", "SET", 31, 61, 0), fact("HEADCOUNT", "MISSION_CONTROL", 0, 0, 5),
                fact("AVERAGE_SKILL", "MISSION_CONTROL", 21, 51, 0)), 1));
        for (int i = 0; i < 2; i++) {
            Personnel person = i == 0 ? pool.getSetSpecialistCandidates().get(2) : pool.getAstronautCandidates().get(4);
            String group = i == 0 ? "SET" : "ASTRONAUT";
            List<de.mkoehler.launchwindow.model.IntelFact> skills = i == 0
                ? List.of(fact("SKILL", "CREWED_SPACECRAFT", 66, 76, 0), fact("SKILL", "ROCKETS", 52, 62, 0),
                    fact("SKILL", "SPACE_PROBES", 40, 50, 0), fact("SKILL", "EVA_SUITS", 22, 32, 0))
                : List.of(fact("SKILL", "LEADERSHIP", 80, 90, 0), fact("SKILL", "PILOTING", 74, 84, 0),
                    fact("SKILL", "EVA", 61, 71, 0), fact("SKILL", "SCIENCE", 30, 40, 0));
            sever.getPersonnelFiles().add(new de.mkoehler.launchwindow.model.PersonnelFile(person.getId(),
                person.getName(), person.getPortraitId(), group, person.getAge(), 28, skills,
                i == 0 ? de.mkoehler.launchwindow.model.Trait.AMBITIOUS : de.mkoehler.launchwindow.model.Trait.GLORY_SEEKER,
                i == 0 ? "Low" : "High", i == 0 ? null : new de.mkoehler.launchwindow.model.Ambition(
                    de.mkoehler.launchwindow.model.Ambition.Kind.FLY_TO_ORBIT, "", 3, 6, 25), i == 0));
        }
        sever.getTidbits().add(new de.mkoehler.launchwindow.model.Tidbit(28, fact("RD_FOCUS", "r7-vostok", 0, 0, 2)));
        sever.getTidbits().add(new de.mkoehler.launchwindow.model.Tidbit(29, fact("HEADCOUNT", "ASTRONAUT", 0, 0, 7)));
        sever.getTidbits().add(new de.mkoehler.launchwindow.model.Tidbit(30,
            fact("COMPONENT_RELIABILITY", "r7-sputnik", 72, 92, 0)));
        sever.getCatches().add(new de.mkoehler.launchwindow.model.IntelCatch(28, true,
            de.mkoehler.launchwindow.model.IntelOperation.Kind.PERSONNEL_FILE, null, 1));
        de.mkoehler.launchwindow.model.RivalFolder meridian = intel.folder("kMeridian", "Meridian Rocket Society");
        meridian.getCatches().add(new de.mkoehler.launchwindow.model.IntelCatch(29, false,
            de.mkoehler.launchwindow.model.IntelOperation.Kind.ASSESSMENT, de.mkoehler.launchwindow.model.IntelArea.RND, 1));
        meridian.learnLevel(1, 29, de.mkoehler.launchwindow.model.RivalFolder.FROM_A_CATCH);
        meridian.getDossiers().put(de.mkoehler.launchwindow.model.IntelArea.PLANS,
            new de.mkoehler.launchwindow.model.Dossier(de.mkoehler.launchwindow.model.IntelArea.PLANS, 27, List.of(
                fact("OPEN_PROGRAM", "explorer-1", 0, 0, 0), fact("OPEN_PROGRAM", "mercury", 0, 0, 0),
                fact("RD_FOCUS", "mercury-capsule", 0, 0, 4), fact("INTEL_LEVEL", "", 0, 0, 1)), 1));
        de.mkoehler.launchwindow.model.RivalFolder aurora = intel.folder("kAurora", "Aurora Bureau");
        aurora.getTidbits().add(new de.mkoehler.launchwindow.model.Tidbit(29, fact("OPEN_PROGRAM", "vostok", 0, 0, 0)));

        startSession(game, state, info);
        List<RivalSummary> rivals = List.of(
            RivalSummary.of("Orion Spaceworks", "A. Reyes", Archetype.NASA, true, Map.of("earth-orbit-satellite", 9),
                "kMe"),
            RivalSummary.of("Sever Design Bureau", "Tamara Abenova", Archetype.SOVIET, false,
                Map.of("earth-orbit-satellite", 8, "human-in-space", 20), "kSever"),
            RivalSummary.of("Aurora Bureau", "Levan Gelashvili", Archetype.SOVIET, false,
                Map.of("earth-orbit-satellite", 10), "kAurora"),
            RivalSummary.of("Meridian Rocket Society", "Walter Krueger", Archetype.NASA, false,
                Map.of("earth-orbit-satellite", 12), "kMeridian"));
        game.session().keepRivals(rivals);
        TurnReport report = new TurnReport(TurnReport.VERSION, 30, 4_310, 4_310, 21_000, 21_640, List.of(), List.of(),
            List.of(
                new de.mkoehler.launchwindow.rules.IntelligenceLevelChanged(1, 2, 3, false),
                new de.mkoehler.launchwindow.rules.IntelligenceFound(new de.mkoehler.launchwindow.rules.IntelFinding(
                    de.mkoehler.launchwindow.rules.IntelFinding.Kind.DOSSIER, "op5",
                    de.mkoehler.launchwindow.model.IntelOperation.Kind.ASSESSMENT, "kSever", "Sever Design Bureau",
                    de.mkoehler.launchwindow.model.IntelArea.PLANS, sever.getDossiers().get(
                        de.mkoehler.launchwindow.model.IntelArea.PLANS), null, null, 1)),
                new de.mkoehler.launchwindow.rules.IntelligenceFound(new de.mkoehler.launchwindow.rules.IntelFinding(
                    de.mkoehler.launchwindow.rules.IntelFinding.Kind.FAILED, "op6",
                    de.mkoehler.launchwindow.model.IntelOperation.Kind.ASSESSMENT, "kAurora", "Aurora Bureau",
                    de.mkoehler.launchwindow.model.IntelArea.RND, null, null, null, -1)),
                new de.mkoehler.launchwindow.rules.IntelligenceFound(new de.mkoehler.launchwindow.rules.IntelFinding(
                    de.mkoehler.launchwindow.rules.IntelFinding.Kind.TIDBIT, null, null, "kSever",
                    "Sever Design Bureau", de.mkoehler.launchwindow.model.IntelArea.RND, null, null,
                    sever.getTidbits().getLast(), -1))));
        game.session().keepReport(report);
        write(folder, "intel-overview.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.OVERVIEW), null);
        write(folder, "intel-operations.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.OPERATIONS), screen -> {
                screen.pickRival("kMeridian");
                screen.pickArea(de.mkoehler.launchwindow.model.IntelArea.PLANS);
            });
        write(folder, "intel-folders.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(game,
            de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.FOLDERS),
            screen -> screen.openFolder("kSever"));
        write(folder, "intel-folders-tidbits-only.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(
            game, de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.FOLDERS),
            screen -> screen.openFolder("kAurora"));
        write(folder, "intel-report.png", new TurnReportScreen(game, report), null);
        write(folder, "intel-hub-hover.png", new SpaceComplexScreen(game),
            screen -> screen.hover(Hub.Place.INTELLIGENCE_OFFICE));

        PlayerSnapshot none = new PlayerSnapshot("Orion Spaceworks", Archetype.NASA, 21_640, 4_310, 3_000, 31);
        none.setDirectorName("A. Reyes");
        startSession(game, none, info);
        game.session().keepRivals(rivals);
        write(folder, "intel-overview-empty.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(
            game, de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.OVERVIEW), null);
        write(folder, "intel-operations-locked.png", new de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen(
            game, de.mkoehler.launchwindow.client.screen.IntelligenceOfficeScreen.Tab.OPERATIONS), null);

        // The hire screen at level 1: a check answered, passive finds (a trait and "no notable
        // traits"), and the check button.
        PlayerSnapshot hiring = setCentreState();
        hiring.getIntelligence().setLevel(1);
        hiring.getIntelligence().setTargetLevel(1);
        List<SetSpecialist> candidates = hiring.getSetSpecialistCandidates();
        candidates.get(0).setTraits(List.of(new de.mkoehler.launchwindow.model.TraitState(
            de.mkoehler.launchwindow.model.Trait.WORKAHOLIC, 0, true)));
        candidates.get(0).setIntelFind(Personnel.FIND_CHECK);
        candidates.get(1).setTraits(List.of(new de.mkoehler.launchwindow.model.TraitState(
            de.mkoehler.launchwindow.model.Trait.STEADY, 0, true)));
        candidates.get(1).setIntelFind(Personnel.FIND_PASSIVE);
        candidates.get(3).setTraits(List.of());
        candidates.get(3).setIntelFind(Personnel.FIND_PASSIVE);
        candidates.get(3).setIntelExhausted(true);
        startSession(game, hiring, info);
        write(folder, "intel-hire-free.png", new HireScreen(game), null);
        game.session().state().getIntelligence().setActionsUsed(1);
        write(folder, "intel-hire-used.png", new HireScreen(game), screen -> screen.toggle(candidates.get(0).getId()));
    }

    private static de.mkoehler.launchwindow.model.IntelFact fact(String kind, String ref, int low, int high, int count) {
        return new de.mkoehler.launchwindow.model.IntelFact(de.mkoehler.launchwindow.model.IntelFact.Kind.valueOf(kind),
            ref, low, high, count);
    }

    /**
     * A mid-game SET Centre: engineers idle, in each kind of training, researching, and
     * unavailable; a candidate pool; and payload components, one at its R&D ceiling.
     */
    private static PlayerSnapshot setCentreState() {
        PlayerSnapshot state = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 24_355, 310, 3_000, 9);
        state.setDirectorName("A. Reyes");
        state.getBuildingLevels().put(Building.SET_CENTER, 3);
        state.getOpenedProgramIds().addAll(List.of("sputnik-1", "vostok"));
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(62.4, 45.0));
        state.getMissionComponents().put("sputnik-2-satellite",
            new OwnedMissionComponent(maxRd("sputnik-2-satellite"), 30.0));
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(54.0, 38.0));
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(70.0, 40.0));

        SetSpecialist idle = specialist("p1", "Elena Markova", 52, 58, 55, 40, 72, 30, 28, 35);
        SetSpecialist basic = specialist("p2", "Josef Adler", 29, 81, 38, 52, 33, 22, 19, 27);
        basic.setAssignmentState(AssignmentState.IN_TRAINING);
        basic.setTrainingKind(TrainingKind.BASIC);
        basic.setTrainingSeasonsTotal(1);
        SetSpecialist researching = specialist("p3", "Mei-Lin Zhou", 44, 67, 47, 35, 29, 41, 78, 45);
        researching.setAssignmentState(AssignmentState.ASSIGNED);
        researching.setRdTargetComponentId("vostok-capsule");
        researching.setRdSlot(0);
        SetSpecialist old = specialist("p4", "Otto Bergmann", 61, 45, 50, 61, 38, 55, 40, 31);
        SetSpecialist advanced = specialist("p5", "Grace Okafor", 35, 74, 42, 30, 58, 24, 22, 33);
        advanced.setAssignmentState(AssignmentState.IN_TRAINING);
        advanced.setTrainingKind(TrainingKind.ADVANCED);
        advanced.setTrainingSeasonsTotal(2);
        SetSpecialist researching2 = specialist("p6", "Hiro Tanaka", 39, 62, 49, 44, 67, 29, 25, 30);
        researching2.setAssignmentState(AssignmentState.ASSIGNED);
        researching2.setRdTargetComponentId("sputnik-1-satellite");
        researching2.setRdSlot(0);
        SetSpecialist away = specialist("p7", "Ines Dubois", 48, 39, 53, 37, 41, 48, 33, 52);
        away.setUnavailableSeasons(2);
        SetSpecialist permanent = specialist("p8", "Rolf Petersen", 33, 70, 44, 48, 30, 71, 26, 24);
        permanent.setAssignmentState(AssignmentState.IN_TRAINING);
        permanent.setTrainingKind(TrainingKind.PERMANENT);
        permanent.setTrainingTargetSkill(SetSkill.HUMAN_RATED_ROCKETS);
        permanent.setTrainingTargetLevel(90);
        state.getSetSpecialists().addAll(List.of(idle, basic, researching, old, advanced, researching2, away, permanent));

        state.getSetSpecialistCandidates().addAll(List.of(
            specialist("c1", "T. Alvarez", 34, 64, 52, 58, 41, 36, 52, 44),
            specialist("c2", "N. Petrova", 29, 71, 47.5, 49, 55, 31, 38, 57),
            specialist("c3", "H. Bergman", 41, 58, 61, 67, 39, 52, 60, 35),
            specialist("c4", "S. Delacroix", 27, 75, 38.5, 44, 48, 28, 33, 41),
            specialist("c5", "K. Osei", 33, 62, 55.5, 53, 46, 47, 56, 62)));
        // c4 keeps no portrait, so the initials fallback stays in the shots.
        return withPortraits(state, "c4");
    }

    /**
     * Gives every fixture person without a portrait one, with the matching name, the way the hiring
     * pool would (ages stay as the fixture set them), except the ids listed.
     *
     * @param state         the fixture
     * @param keepInitials  ids to leave without a portrait
     * @return the same fixture
     */
    private static PlayerSnapshot withPortraits(PlayerSnapshot state, String... keepInitials) {
        Set<String> skip = Set.of(keepInitials);
        Set<String> taken = new HashSet<>();
        Map<PersonnelGroup, List<? extends Personnel>> people = new EnumMap<>(PersonnelGroup.class);
        List<Personnel> astronauts = new ArrayList<>(state.getAstronauts());
        astronauts.addAll(state.getAstronautCandidates());
        List<Personnel> set = new ArrayList<>(state.getSetSpecialists());
        set.addAll(state.getSetSpecialistCandidates());
        List<Personnel> controllers = new ArrayList<>(state.getMissionControlOfficers());
        controllers.addAll(state.getMissionControlCandidates());
        people.put(PersonnelGroup.ASTRONAUT, astronauts);
        people.put(PersonnelGroup.SET, set);
        people.put(PersonnelGroup.MISSION_CONTROL, controllers);
        people.forEach((group, list) -> {
            for (Personnel person : list) {
                if (person.getPortraitId() != null || skip.contains(person.getId())) {
                    continue;
                }
                PersonnelNames.Identity identity = PersonnelNames.forCandidate(person.getId(), state.getArchetype(),
                    "Screenshots", group, taken, Set.of());
                taken.add(identity.portraitId());
                person.setPortraitId(identity.portraitId());
                person.setName(identity.name());
            }
        });
        return state;
    }

    /**
     * A Soviet agency with Sputnik 1 and Korabl-Sputnik 2 open (its Vostok capsule owned), Sputnik 2's
     * satellite kept from an earlier opening, and the first satellite goal achieved.
     */
    private static PlayerSnapshot headquartersState() {
        PlayerSnapshot state = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 24_355, 850, 3_000, 13);
        state.setDirectorName("A. Reyes");
        state.getOpenedProgramIds().addAll(List.of("sputnik-1", "korabl-sputnik-2"));
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(58.4, 3.0));
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(47.2, 3.0));
        OwnedMissionComponent kept = new OwnedMissionComponent(22.0, 3.0);
        kept.setActive(false);
        state.getMissionComponents().put("sputnik-2-satellite", kept);
        state.getAchievedGoals().put("earth-orbit-satellite", 4);
        return state;
    }

    private static void planningShots(File folder, LaunchWindowGame game, GameInfoResponse info) {
        startSession(game, planningState(11, 25_000), info);
        write(folder, "plan-configs.png", new MissionConfigsScreen(game, "vostok"), null);
        write(folder, "plan-steps.png", new MissionStepsScreen(game, "vostok-orbital"), null);
        write(folder, "plan-steps-one-season.png", new MissionStepsScreen(game, "sputnik-1-orbit"), null);
        write(folder, "plan-steps-extended.png", new MissionStepsScreen(game, "sputnik-1-extended"), null);
        write(folder, "plan-assessment.png", new MissionPlanScreen(game, "vostok-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);
        write(folder, "plan-rockets.png", new MissionPlanScreen(game, "vostok-orbital", MissionPlanScreen.Tab.ROCKETS),
            null);
        write(folder, "plan-rnd.png", new MissionPlanScreen(game, "vostok-orbital", MissionPlanScreen.Tab.RND), null);
        write(folder, "plan-assemble.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        write(folder, "plan-assemble-capcom-note.png", new AssembleMissionScreen(game, "vostok-orbital"),
            screen -> pinLastHistoryNote(screen.stage().getRoot()));
        write(folder, "plan-assemble-picker.png", new AssembleMissionScreen(game, "vostok-orbital"),
            screen -> screen.openPicker(false, 3));
        write(folder, "plan-assemble-confirm.png", new AssembleMissionScreen(game, "vostok-orbital"),
            screen -> screen.confirmLaunch(game.session().draft(game.session().catalog()
                .missionConfiguration("vostok-orbital").orElseThrow()), 4_369));
        startSession(game, planningState(11, 25_000), info);
        write(folder, "plan-assemble-launched.png", new AssembleMissionScreen(game, "vostok-orbital"),
            AssembleMissionScreen::previewLaunched);

        PlayerSnapshot penalty = planningState(11, 25_000);
        penalty.getCancelledGoalIds().add("human-in-space");
        startSession(game, penalty, info);
        write(folder, "plan-assessment-no-penalty.png", new MissionPlanScreen(game, "vostok-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);

        startSession(game, planningState(5, 25_000), info);
        write(folder, "plan-assemble-short.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        write(folder, "plan-assessment-short.png", new MissionPlanScreen(game, "vostok-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);
        startSession(game, planningState(11, 2_000), info);
        write(folder, "plan-assemble-funds.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        PlayerSnapshot capped = planningState(11, 25_000);
        capped.setLaunchCap(1);
        capped.setLaunchesThisWindow(1);
        startSession(game, capped, info);
        write(folder, "plan-assemble-cap.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        PlayerSnapshot grounded = planningState(11, 25_000);
        grounded.getUnavailableComponents().put("vostok-capsule", 1);
        startSession(game, grounded, info);
        write(folder, "plan-assemble-grounded.png", new AssembleMissionScreen(game, "vostok-orbital"), null);

        startSession(game, headquartersState(), info);
        write(folder, "plan-configs-readonly.png", new MissionConfigsScreen(game, "vostok"), null);
        write(folder, "plan-assessment-readonly.png", new MissionPlanScreen(game, "vostok-orbital",
            MissionPlanScreen.Tab.ASSESSMENT), null);
    }

    private static void missionControlShots(File folder, LaunchWindowGame game, GameInfoResponse info) {
        startSession(game, missionControlState(), info);
        InProgressMission scheduled = game.session().state().getInProgressMissions().values().stream()
            .filter(mission -> mission.getCurrentSeasonIndex() == 0).findFirst().orElseThrow();
        write(folder, "mc-overview.png", new MissionControlScreen(game), null);
        write(folder, "mc-manage.png", new MissionControlScreen(game), MissionControlScreen::openManageDepartment);
        write(folder, "mc-train.png", new MissionControlScreen(game), screen -> screen.openTraining(
            game.session().state().getMissionControlOfficers().stream()
                .filter(officer -> officer.getAssignmentState() == AssignmentState.IDLE).reduce((a, b) -> b)
                .orElseThrow()));
        write(folder, "mc-hire.png", new HireScreen(game, Department.MISSION_CONTROL), null);
        write(folder, "mc-hire-tooltip-fd.png", new HireScreen(game, Department.MISSION_CONTROL),
            screen -> showTooltip(screen.stage().getRoot(), "Flight Director"));
        write(folder, "mc-overview-tooltip-footer.png", new MissionControlScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "Flight controllers: "));
        write(folder, "mc-overview-tooltip-skill.png", new MissionControlScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "Propulsion"));
        write(folder, "mc-overview-tooltip-morale.png", new MissionControlScreen(game),
            screen -> showTooltip(screen.stage().getRoot(), "Morale"));
        write(folder, "mc-scheduled.png", new MissionsScreen(game, true), null);
        write(folder, "mc-scrub.png", new MissionsScreen(game, true), screen -> screen.confirmCallOff(scheduled));
        write(folder, "mc-active.png", new MissionsScreen(game, false), null);
        write(folder, "mc-cancel.png", new MissionsScreen(game, false), screen -> screen.confirmCallOff(
            game.session().state().getInProgressMissions().get("m-sputnik")));
        write(folder, "mc-restaff.png", new RestaffScreen(game, scheduled.getId()), null);
        write(folder, "mc-restaff-picker.png", new RestaffScreen(game, scheduled.getId()),
            screen -> screen.openPicker(0));
        write(folder, "mc-restaff-active.png", new RestaffScreen(game, "m-sputnik"), null);
        startSession(game, planningState(11, 25_000), info);
        write(folder, "mc-scheduled-empty.png", new MissionsScreen(game, true), null);
        write(folder, "mc-active-empty.png", new MissionsScreen(game, false), null);
    }

    /**
     * The planning agency right after launching Vostok Orbital, with Sputnik 1 Extended in its
     * last season: BOOSTER was released after season 1, SPACECRAFT SYSTEMS is still staffed. One
     * controller holds an Expert skill, and two candidates wait in the pool.
     */
    private static PlayerSnapshot missionControlState() {
        PlayerSnapshot state = planningState(13, 25_000);
        state.getBuildingLevels().put(Building.MISSION_CONTROL, 3);
        ContentCatalog catalog = ContentPackLoader.loadFromClasspath("content/early-era",
            ScreenSnapshot.class.getClassLoader());
        MissionConfiguration orbital = catalog.missionConfiguration("vostok-orbital").orElseThrow();
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(state, orbital);
        MissionLaunch.launch(state, new LaunchRequest(orbital.id(), staffing.crewSlotIds(), staffing.controlSlotIds(),
            List.of("r7-vostok")), catalog);
        MissionControlOfficer systems = state.getMissionControlOfficers().stream()
            .filter(officer -> officer.getAssignmentState() == AssignmentState.IDLE).findFirst().orElseThrow();
        // BOOSTER was released after season 1: the season pass emptied the console then.
        InProgressMission sputnik = new InProgressMission("m-sputnik", "sputnik-1-extended", List.of(),
            Arrays.asList(null, systems.getId(), null));
        sputnik.setRocketIds(List.of("r7-sputnik"));
        sputnik.setLaunchCost(2_113);
        sputnik.setCurrentSeasonIndex(2);
        state.getInProgressMissions().put("m-sputnik", sputnik);
        systems.setAssignmentState(AssignmentState.ASSIGNED);
        systems.setAssignedMissionId("m-sputnik");
        state.getMissionControlOfficers().get(0).getExpertSkills().add(MissionControlSkill.PROPULSION);
        for (int i = 0; i < 2; i++) {
            MissionControlOfficer candidate = new MissionControlOfficer("cand" + i, i == 0 ? "Pyotr Lavrov"
                : "Irina Volkova", 27 + i, 60, 70);
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                candidate.setSkill(skill, 40 + i * 9 + skill.ordinal() * 4);
            }
            candidate.setSalary(31 + i * 4);
            state.getMissionControlCandidates().add(candidate);
        }
        return withPortraits(state);
    }

    /**
     * The turn report and the replay, from a real season pass: the Mission Control agency resolves
     * one turn with Vostok Orbital and Sputnik 1 Extended flying.
     */
    private static void reportShots(File folder, LaunchWindowGame game, GameInfoResponse info) {
        TurnReport report;
        PlayerSnapshot after;
        try {
            Path playerRoot = Files.createTempDirectory("snapshot-player");
            ObjectMapper mapper = new ObjectMapper();
            ContentCatalog catalog = ContentPackLoader.loadFromClasspath("content/early-era",
                ScreenSnapshot.class.getClassLoader());
            ActionContext context = new ActionContext(catalog, BudgetSchedule.baspmClose());
            PlayerSnapshot before = missionControlState();
            // The fixture never had a review: pay for more prestige than this turn reaches, so no raise
            // between reviews (design.md 2.1(f)) comes into this report; raise-report.png shows one.
            before.setFundedPrestige(1_000_000);
            new PlayerSnapshotStore(playerRoot, mapper).save(0, before, null);
            PlayerStateService service = new PlayerStateService(playerRoot, mapper, context);
            service.resolveTurn(23, new GoalTally(), new RandomEventRoster(100, 100,
                RandomEventRoster.baspmClose().events()), REPORT_SEED);
            report = service.turnReport(23).orElseThrow();
            after = service.currentState();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        GameInfoResponse next = GameInfoResponse.success("early-era", info.contentPackHash(), List.of(), 24, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        startSession(game, after, next);
        game.session().keepReport(report);
        write(folder, "report.png", new TurnReportScreen(game, report), null);
        // The news, with each random event's story (2026-10-10): every column scrolled to its end.
        write(folder, "report-news.png", new TurnReportScreen(game, report), screen -> scrollToEnd(screen.stage()
            .getRoot()));
        String vostok = report.missions().stream().filter(flown -> flown.missionConfigurationId()
            .equals("vostok-orbital")).findFirst().orElseThrow().missionInstanceId();
        // A step while its clip plays (the result not shown yet), then once the clip has made it clear.
        write(folder, "replay-step-watching.png", new ReplayScreen(game, report, vostok), screen -> screen.show(
            firstInteresting(report, vostok)));
        write(folder, "replay-step.png", new ReplayScreen(game, report, vostok), screen -> {
            screen.show(firstInteresting(report, vostok));
            screen.watchPaused(10f);
        });
        write(folder, "replay-summary.png", new ReplayScreen(game, report, vostok), screen -> screen.show(99));
        // The Quindar tones' note on the console's air-to-ground label, pinned open (2026-10-18).
        write(folder, "replay-quindar-note.png", new ReplayScreen(game, report, vostok), screen -> {
            screen.show(firstInteresting(report, vostok));
            pinLastHistoryNote(screen.stage().getRoot());
        });
        write(folder, "replay-sputnik.png", new ReplayScreen(game, report, "m-sputnik"), screen -> screen.show(0));
        // The console live (2026-10-13): Vostok's launch with a stand-in video, while it plays and after.
        fakeVideo = true;
        List<MissionStepResolved> vostokSteps = TurnReportView.steps(report, vostok);
        int launch = java.util.stream.IntStream.range(0, vostokSteps.size())
            .filter(i -> vostokSteps.get(i).stepId().endsWith("launch")).findFirst().orElse(0);
        write(folder, "replay-launch-watching.png", new ReplayScreen(game, report, vostok), screen -> {
            screen.show(launch);
            screen.render(1.5f);
        });
        write(folder, "replay-launch.png", new ReplayScreen(game, report, vostok), screen -> {
            screen.show(launch);
            screen.watchPaused(5f);
        });
        fakeVideo = false;
        // Watching the turn's missions first (ui-spec.md 4.4, 2026-10-12): the offer over the hub, and the
        // end of a mission in the queue, with the next one offered.
        write(folder, "replays-offer.png", new SpaceComplexScreen(game), screen -> screen.offerReplays(report));
        List<String> queue = report.missions().stream().map(TurnReport.MissionFlown::missionInstanceId).toList();
        write(folder, "replay-queue-summary.png", new ReplayScreen(game, report, queue.get(0), queue),
            screen -> screen.show(99));
        // The same report a turn later: an earlier report, with Next offered and no morale warnings.
        GameInfoResponse later = GameInfoResponse.success("early-era", info.contentPackHash(), List.of(), 25, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        startSession(game, after, later);
        game.session().keepReport(report);
        write(folder, "report-earlier.png", new TurnReportScreen(game, report), null);
    }

    /** The game's default declared goals, as the Museum mockup shows them. */
    private static final List<String> DECLARED = List.of("earth-orbit-satellite", "human-in-space", "human-in-orbit");

    /**
     * The Museum, Settings and the game result, after the mockup's turn: Vostok Orbital has just
     * landed at turn 24, which finished the declared goals; three rivals, one astronaut lost.
     */
    private static void museumShots(File folder, LaunchWindowGame game, GameInfoResponse info) {
        PlayerSnapshot state = museumState();
        String hash = ContentPackHash.ofClasspath("content/early-era", ScreenSnapshot.class.getClassLoader());
        GameInfoResponse won = GameInfoResponse.success("early-era", hash, DECLARED, 25, true,
            23 * 3_600_000L + 40 * 60_000L, 0L, "WON", List.of(state.getAgencyName()), List.of(state.getDirectorName()),
            24);
        startSession(game, state, won);
        RivalSummary me = RivalSummary.of(state.getAgencyName(), state.getDirectorName(), Archetype.SOVIET, true,
            Map.of("earth-orbit-satellite", 12, "human-in-space", 24, "human-in-orbit", 24));
        List<RivalSummary> rivals = List.of(me,
            RivalSummary.of("Orion Spaceworks", "Ana Reyes", Archetype.NASA, false, Map.of("earth-orbit-satellite", 11)),
            RivalSummary.of("Kestrel Aerospace", "Moses Okafor", Archetype.NASA, false,
                Map.of("earth-orbit-satellite", 13)),
            RivalSummary.of("Polar Star Bureau", "Vera Sedova", Archetype.SOVIET, false,
                Map.of("earth-orbit-satellite", 14)));
        game.session().keepRivals(rivals);
        write(folder, "museum-race.png", new MuseumScreen(game, MuseumScreen.Tab.RACE), null);
        write(folder, "museum-achievements.png", new MuseumScreen(game, MuseumScreen.Tab.ACHIEVEMENTS), null);
        write(folder, "museum-flights.png", new MuseumScreen(game, MuseumScreen.Tab.FLIGHTS), null);
        write(folder, "museum-memorial.png", new MuseumScreen(game, MuseumScreen.Tab.MEMORIAL), null);
        write(folder, "settings.png", new SpaceComplexScreen(game), SpaceComplexScreen::openSettings);
        write(folder, "settings-chrome.png", new MuseumScreen(game, MuseumScreen.Tab.FLIGHTS),
            MuseumScreen::openSettings);
        write(folder, "settings-credits.png", new SpaceComplexScreen(game), SpaceComplexScreen::openCredits);
        write(folder, "result-won.png", new SpaceComplexScreen(game), SpaceComplexScreen::showResult);

        // A rival won on the medal table: two agencies finished at turn 24.
        GameInfoResponse medal = GameInfoResponse.success("early-era", hash, DECLARED, 25, true,
            23 * 3_600_000L, 0L, "WON", List.of("Orion Spaceworks"), List.of("Ana Reyes"), 24);
        startSession(game, state, medal);
        game.session().keepRivals(List.of(me,
            RivalSummary.of("Orion Spaceworks", "Ana Reyes", Archetype.NASA, false,
                Map.of("earth-orbit-satellite", 11, "human-in-space", 20, "human-in-orbit", 24)),
            rivals.get(2), rivals.get(3)));
        write(folder, "result-medal.png", new SpaceComplexScreen(game), SpaceComplexScreen::showResult);

        // An exact tie: level on every place of the medal table.
        GameInfoResponse tie = GameInfoResponse.success("early-era", hash, DECLARED, 25, true,
            23 * 3_600_000L, 0L, "TIED", List.of(state.getAgencyName(), "Orion Spaceworks"),
            List.of(state.getDirectorName(), "Ana Reyes"), 24);
        startSession(game, state, tie);
        game.session().keepRivals(List.of(me,
            RivalSummary.of("Orion Spaceworks", "Ana Reyes", Archetype.NASA, false,
                Map.of("earth-orbit-satellite", 12, "human-in-space", 24, "human-in-orbit", 24)),
            rivals.get(2), rivals.get(3)));
        write(folder, "result-tie.png", new SpaceComplexScreen(game), SpaceComplexScreen::showResult);

        // No losses yet: the memorial's empty state.
        PlayerSnapshot spared = museumState();
        spared.setMemorial(List.of());
        startSession(game, spared, won);
        write(folder, "museum-memorial-empty.png", new MuseumScreen(game, MuseumScreen.Tab.MEMORIAL), null);

        // A dev-mode server: the hub strip offers to resolve the turn now.
        startSession(game, spared, GameInfoResponse.success("early-era", hash, DECLARED, 25, true,
            23 * 3_600_000L + 40 * 60_000L, 0L, null, List.of(), List.of(), 0, true));
        write(folder, "hub-dev-mode.png", new SpaceComplexScreen(game), null);

        departmentShots(folder, game, hash);
    }

    /**
     * The Astronaut Complex and the Public Affairs Office, on the Mission Control agency (Vostok
     * Orbital scheduled, so one cosmonaut flies and one sits at CAPCOM).
     */
    private static void departmentShots(File folder, LaunchWindowGame game, String hash) {
        PlayerSnapshot state = missionControlState();
        List<Astronaut> idle = state.getAstronauts().stream()
            .filter(astronaut -> astronaut.getAssignmentState() == AssignmentState.IDLE).toList();
        Astronaut eligible = idle.get(0);
        eligible.setSkill(AstronautSkill.FITNESS, 81.0);
        Astronaut expert = idle.get(1);
        expert.setSkill(AstronautSkill.LEADERSHIP, 86.0);
        expert.getExpertSkills().add(AstronautSkill.LEADERSHIP);
        for (int i = 0; i < state.getAstronauts().size(); i++) {
            state.getAstronauts().get(i).setSalary(42 + i * 4);
        }
        state.getBuildingLevels().put(Building.ASTRONAUT_CENTER, 2);
        for (Astronaut astronaut : state.getAstronauts()) {
            if (astronaut.getAssignmentState() == AssignmentState.IN_TRAINING) {
                astronaut.setTrainingKind(TrainingKind.BASIC);
                astronaut.setTrainingTargetSkill(AstronautSkill.EVA);
                astronaut.setTrainingSeasonsTotal(3);
                astronaut.setTrainingSeasonsCompleted(1);
            }
        }
        GameInfoResponse info = GameInfoResponse.success("early-era", hash, DECLARED, 23, true,
            6 * 3_600_000L + 12 * 60_000L + 30_000L, 0L, null, List.of(), List.of(), 0);
        startSession(game, state, info);
        write(folder, "ac-overview.png", new AstronautComplexScreen(game), null);
        write(folder, "ac-train-expert.png", new AstronautComplexScreen(game),
            screen -> screen.openTraining(eligible, TrainingKind.EXPERT, "FITNESS"));
        write(folder, "ac-train-expert-none.png", new AstronautComplexScreen(game),
            screen -> screen.openTraining(expert, TrainingKind.EXPERT, null));
        write(folder, "ac-upgrade.png", new AstronautComplexScreen(game),
            screen -> screen.confirmUpgrade(Building.ASTRONAUT_CENTER, "the Astronaut Complex", "headcount cap"));
        Astronaut onMission = state.getAstronauts().stream()
            .filter(astronaut -> astronaut.getAssignmentState() == AssignmentState.ASSIGNED).findFirst().orElseThrow();
        write(folder, "ac-dismiss-refused.png", new AstronautComplexScreen(game),
            screen -> screen.confirmDismiss(onMission));

        // The Public Affairs Office: a news log, then a debt with the convert dialog, then both timed effects.
        // With stories (2026-10-10); the unavailable one has none, as an entry from before them.
        state.setNewsLog(List.of(new NewsEntry(17, RandomEventKind.MORALE_BOOST, "SET", 12, "mb-families-visit", null),
            new NewsEntry(20, RandomEventKind.EMPLOYEE_UNAVAILABLE, null, 1),
            new NewsEntry(22, RandomEventKind.UNEXPECTED_COST, null, 9.3, "uc-lawsuit", null)));
        write(folder, "pao.png", new PublicAffairsScreen(game), null);
        PlayerSnapshot debt = missionControlState();
        debt.setFunds(-1_200);
        debt.setPrestige(2_140);
        debt.getBuildingLevels().put(Building.ASTRONAUT_CENTER, 3);
        debt.getBuildingLevels().put(Building.MISSION_CONTROL, 3);
        startSession(game, debt, info);
        write(folder, "pao-debt.png", new PublicAffairsScreen(game), null);
        write(folder, "pao-convert.png", new PublicAffairsScreen(game), PublicAffairsScreen::openConvert);
        PlayerSnapshot effects = missionControlState();
        effects.setCostMultiplier(0.85, 5);
        effects.setRdResults(0.9, 2);
        effects.setNewsLog(List.of(new NewsEntry(22, RandomEventKind.REDUCED_EXPENDITURES, null, 0.85),
            new NewsEntry(22, RandomEventKind.RD_RESULTS_SETBACK, null, 0.9)));
        startSession(game, effects, info);
        write(folder, "pao-effects.png", new PublicAffairsScreen(game), null);

        SilentGateway gateway = (SilentGateway) game.gateway();
        gateway.waiting = true;
        write(folder, "waiting-for-server.png", new PublicAffairsScreen(game), screen -> screen.render(0.6f));
        write(folder, "waiting-for-server-hub.png", new SpaceComplexScreen(game), screen -> screen.render(0.6f));
        gateway.waiting = false;

        // PR duty (2026-10-06): a flown veteran on tour, an idle astronaut about to go, the Public Affairs
        // Office's PR line, and a report with a quiet tour and one that made the papers.
        expert.setFlightsFlown(1);
        expert.setFirstsFlown(1);
        expert.setAssignmentState(AssignmentState.PR_DUTY);
        expert.setPrDutySeasons(3);
        startSession(game, state, info);
        write(folder, "ac-pr-tour.png", new AstronautComplexScreen(game), null);
        write(folder, "ac-pr-dialog.png", new AstronautComplexScreen(game), screen -> screen.openPrDuty(eligible));
        write(folder, "pao-pr.png", new PublicAffairsScreen(game), null);
        TurnReport prReport = new TurnReport(TurnReport.VERSION, 22, 2_100, 2_140, 12_000, 12_400, List.of(),
            List.of(), List.of(new PrDutyPerformed(expert.getId(), expert.getName(), 16, 24, "applications-soar", 1.0,
                List.of(new PrPress.Effect(PrPress.EffectKind.HIRING_POOL, "SET", 3, 2))),
            new PrDutyPerformed(eligible.getId(), eligible.getName(), 6, 6, null, 0.0)));
        game.session().keepReport(prReport);
        write(folder, "report-pr.png", new TurnReportScreen(game, prReport), null);

        // Press-story effects (2026-10-06): a SET pool grown by three, one of them a standout.
        game.session().state().getHiringPoolBonuses().add(new HiringPoolBonus("SET", 3, 1, 1));
        HiringPool.refreshCandidates(game.session().state(), new java.util.Random(7));
        write(folder, "hire-press-bonus.png", new HireScreen(game), null);

        // The Museum encyclopedia (2026-10-06): its tab, and every sheet of every explainer.
        write(folder, "museum-encyclopedia.png", new MuseumScreen(game, MuseumScreen.Tab.ENCYCLOPEDIA), null);
        explainerShots(folder, game);

        // Concept chips (2026-10-07): the loop above opened every explainer, so all read now. The
        // unread shots are the earlier hq-details-vostok, plan-steps, vab-open and museum-encyclopedia.
        write(folder, "concepts-program-read.png", new ProgramDetailsScreen(game, "vostok", CelestialBody.EARTH), null);
        write(folder, "concepts-config-read.png", new MissionStepsScreen(game, "vostok-orbital"), null);
        write(folder, "concepts-museum-read.png", new MuseumScreen(game, MuseumScreen.Tab.ENCYCLOPEDIA), null);
        game.useExplainerReadMarksInMemory(new java.util.HashSet<>(java.util.Set.of("rendezvous")));
        write(folder, "concepts-config-partly-read.png", new MissionStepsScreen(game, "mercury-joint-orbital"), null);
        // The pointer's states: the first chip hovered, the second pressed.
        write(folder, "concepts-chip-states.png", new ProgramDetailsScreen(game, "vostok", CelestialBody.EARTH),
            screen -> pointAtChips(screen.stage().getRoot()));
    }

    /** The mockup's agency at 1960 · S4, with the goal records and memorial the Museum reads. */
    private static PlayerSnapshot museumState() {
        PlayerSnapshot state = new PlayerSnapshot("Zarya Design Bureau", Archetype.SOVIET, 15_275, 10_190, 3_000, 25);
        state.setDirectorName("Nina Tarasova");
        state.getOpenedProgramIds().addAll(List.of("sputnik-1", "vostok"));
        state.setAchievedGoals(Map.of("sputnik-1-flown", 12, "earth-orbit-satellite", 12,
            "vostok-uncrewed-suborbital-flown", 18, "vostok-uncrewed-orbital-flown", 21, "vostok-orbital-flown", 24));
        state.setCancelledGoalIds(new java.util.LinkedHashSet<>(List.of("radio-signals-from-orbit",
            "vostok-suborbital-flown", "human-in-orbit", "human-in-space")));
        state.setGoalRecords(List.of(
            new GoalRecord("sputnik-1-flown", 12, "sputnik-1-orbit", null, true, 300),
            new GoalRecord("earth-orbit-satellite", 12, "sputnik-1-orbit", null, false, 300),
            new GoalRecord("radio-signals-from-orbit", 12, "sputnik-1-orbit", "earth-orbit-satellite", false, 125),
            new GoalRecord("vostok-uncrewed-suborbital-flown", 18, "vostok-uncrewed-suborbital", null, true, 120),
            new GoalRecord("vostok-uncrewed-orbital-flown", 21, "vostok-uncrewed-orbital", null, true, 150),
            new GoalRecord("vostok-orbital-flown", 24, "vostok-orbital", null, true, 1_200),
            new GoalRecord("vostok-suborbital-flown", 24, "vostok-orbital", "vostok-orbital-flown", true, 600),
            new GoalRecord("human-in-space", 24, "vostok-orbital", "vostok-orbital-flown", true, 1_750),
            new GoalRecord("human-in-orbit", 24, "vostok-orbital", "vostok-orbital-flown", true, 3_000)));
        state.setSuccessfulFlightCounts(Map.of("sputnik-1-orbit", 2, "sputnik-1-extended", 1,
            "vostok-uncrewed-suborbital", 1, "vostok-uncrewed-orbital", 2, "vostok-orbital", 1, "korabl-sputnik-2-orbit", 1));
        state.setMemorial(List.of(new MemorialEntry("Laika", 0, 4, MemorialEntry.Cause.PASSENGER, "sputnik-2-orbit",
            null, "laika"), new MemorialEntry("Arseny Kolesov", 27, 19, MemorialEntry.Cause.ACCIDENT, null, null,
            "soviet_051"), new MemorialEntry("Pavel Orlov", 31, 22, MemorialEntry.Cause.MISSION, "vostok-orbital",
            "11-reentry", "soviet_019")));
        return state;
    }

    /** The seed for the report fixture's season pass. */
    private static final long REPORT_SEED = 11L;

    /** Picks the first step that needed a saving roll, or the first step. */
    private static int firstInteresting(TurnReport report, String missionId) {
        List<MissionStepResolved> steps = TurnReportView.steps(report, missionId);
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).savingChance() != null) {
                return i;
            }
        }
        return 0;
    }

    private static void startSession(LaunchWindowGame game, PlayerSnapshot state, GameInfoResponse info) {
        game.setSession(ClientSession.start(CREDENTIALS, state, new GameInfo(info, System.currentTimeMillis()),
            ScreenSnapshot.class.getClassLoader()));
    }

    /**
     * The mockup's agency in 1960: Vostok open, both uncrewed Vostok flights flown, nobody in
     * space yet, six cosmonauts (one in training) and some controllers (one in training).
     */
    private static PlayerSnapshot planningState(int controllers, long funds) {
        PlayerSnapshot state = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, funds, 2_140, 3_000, 23);
        state.setDirectorName("A. Reyes");
        state.getOpenedProgramIds().addAll(List.of("sputnik-1", "korabl-sputnik-2", "vostok"));
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(72.0, 3.0));
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(80.6, 8.7));
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(84.3, 3.0));
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(78.2, 3.0));
        for (String goal : List.of("earth-orbit-satellite", "sputnik-1-flown", "korabl-sputnik-2-flown",
            "vostok-uncrewed-suborbital-flown", "vostok-uncrewed-orbital-flown")) {
            state.getAchievedGoals().put(goal, 10);
        }
        state.getCancelledGoalIds().add("radio-signals-from-orbit");
        state.getSuccessfulFlightCounts().put("vostok-uncrewed-suborbital", 1);
        state.getSuccessfulFlightCounts().put("vostok-uncrewed-orbital", 1);
        String[][] astronauts = {{"Pavel Orlov", "58", "71", "34", "77", "22"}, {"Andrei Volkov", "66", "52", "41",
            "69", "30"}, {"Nikolai Belov", "49", "63", "55", "61", "18"}, {"Grigori Zaitsev", "72", "44", "38", "58",
            "26"}, {"Mikhail Ermakov", "41", "59", "29", "72", "35"}, {"Viktor Dorofeev", "61", "66", "47", "74", "40"}};
        for (int i = 0; i < astronauts.length; i++) {
            Astronaut astronaut = new Astronaut("a" + i, astronauts[i][0], 30 + i, 70, 60);
            astronaut.setSkill(AstronautSkill.LEADERSHIP, Double.parseDouble(astronauts[i][1]));
            astronaut.setSkill(AstronautSkill.PILOTING, Double.parseDouble(astronauts[i][2]));
            astronaut.setSkill(AstronautSkill.SCIENCE, Double.parseDouble(astronauts[i][3]));
            astronaut.setSkill(AstronautSkill.FITNESS, Double.parseDouble(astronauts[i][4]));
            astronaut.setSkill(AstronautSkill.EVA, Double.parseDouble(astronauts[i][5]));
            astronaut.setAssignmentState(i == 4 ? AssignmentState.IN_TRAINING : AssignmentState.IDLE);
            state.getAstronauts().add(astronaut);
        }
        String[] names = {"Sergei Markov", "Oleg Fedorov", "Ivan Kozlov", "Boris Antonov", "Lev Romanov",
            "Arkady Smirnov", "Konstantin Pavlov", "Valentina Kirova", "Tamara Sorokina", "Fyodor Gusev",
            "Raisa Melnikova", "Yuri Panin", "Pavel Lazarev"};
        for (int i = 0; i < controllers; i++) {
            MissionControlOfficer officer = new MissionControlOfficer("c" + i, names[i], 35 + i, 70, 60);
            officer.setSalary(38 + i * 3);
            int k = 0;
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                officer.setSkill(skill, 45 + (i * 7 + k++ * 11) % 35);
            }
            officer.setAssignmentState(i == controllers - 1 && controllers > 5 ? AssignmentState.IN_TRAINING
                : AssignmentState.IDLE);
            state.getMissionControlOfficers().add(officer);
        }
        SetSpecialist researcher = specialist("s1", "Otto Bergmann", 44, 66, 50, 72, 30, 41, 64, 18);
        researcher.setAssignmentState(AssignmentState.ASSIGNED);
        researcher.setRdTargetComponentId("vostok-capsule");
        researcher.setRdSlot(0);
        state.getSetSpecialists().add(researcher);
        state.getSetSpecialists().add(specialist("s2", "Elena Markova", 38, 70, 55, 64, 28, 52, 58, 35));
        return state;
    }

    /**
     * A Soviet agency with Sputnik 1 open and its R-7 rocket open, flying, and researched by one
     * engineer. With {@code bothOpen}, the R-7 Vostok is open too and the VAB is upgrading;
     * without, the R-7 Vostok is kept from a closed program.
     */
    private static PlayerSnapshot vabState(boolean bothOpen) {
        PlayerSnapshot state = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 24_355, 850, 3_000, 13);
        state.setDirectorName("A. Reyes");
        state.getOpenedProgramIds().add("sputnik-1");
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(58.4, 3.0));
        state.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(61.5, 40.0));
        OwnedMissionComponent vostok = new OwnedMissionComponent(47.0, 35.0);
        vostok.setActive(bothOpen);
        state.getMissionComponents().put("r7-vostok", vostok);
        InProgressMission mission = new InProgressMission("m1", "sputnik-1-orbit", List.of(), List.of());
        mission.setRocketIds(List.of("r7-sputnik"));
        state.getInProgressMissions().put("m1", mission);
        SetSpecialist researcher = specialist("s1", "Otto Bergmann", 44, 66, 50, 72, 30, 41, 22, 18);
        researcher.setAssignmentState(AssignmentState.ASSIGNED);
        researcher.setRdTargetComponentId("r7-sputnik");
        researcher.setRdSlot(0);
        state.getSetSpecialists().add(researcher);
        state.getSetSpecialists().add(specialist("s2", "Elena Markova", 38, 70, 55, 64, 28, 52, 30, 35));
        if (bothOpen) {
            state.getBuildingUpgradeSeasonsRemaining().put(Building.VEHICLE_ASSEMBLY, 1);
        }
        return state;
    }

    /**
     * People worth caring about (2026-10-07, People canvas): rosters with trait chips, person pages,
     * the R&amp;D board's chemistry and its breakdown, the assign dialog's previews, and hire hints.
     */
    private static void peopleShots(File folder, LaunchWindowGame game, GameInfoResponse info) {
        PlayerSnapshot state = peopleState();
        startSession(game, state, info);
        List<SetSpecialist> set = state.getSetSpecialists();
        write(folder, "people-set-roster.png", new SetCentreScreen(game), null);
        write(folder, "people-person-veteran.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game,
            set.get(0), () -> new SetCentreScreen(game)), null);
        write(folder, "people-person-new.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game,
            set.get(5), () -> new SetCentreScreen(game)), null);
        write(folder, "people-person-settled.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game,
            set.get(4), () -> new SetCentreScreen(game)), null);
        write(folder, "people-trait-tooltip.png", new de.mkoehler.launchwindow.client.screen.PersonScreen(game,
            set.get(0), () -> new SetCentreScreen(game)), screen -> showTooltip(screen.stage().getRoot(), "Mentor"));
        write(folder, "people-rnd.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), null);
        write(folder, "people-rnd-breakdown.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD),
            screen -> pinFirstBreakdown(screen.stage().getRoot()));
        write(folder, "people-rnd-assign.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD),
            screen -> screen.openAssign(PayloadRnd.cards(game.session().state(), game.session().catalog()).get(1)));
        write(folder, "people-hire.png", new HireScreen(game), null);
        // Past the cap the player knows of with people still seated: a hidden Perfectionist's early tell.
        double sputnikCap = maxRd("sputnik-1-satellite");
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(sputnikCap + 1.1, 45.0));
        write(folder, "people-rnd-hidden-cap.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), null);
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(62.4, 45.0));

        // Headquarters: a crew and consoles with traits, a clash and a friendship.
        PlayerSnapshot planning = planningState(11, 25_000);
        for (Personnel person : java.util.stream.Stream.concat(planning.getAstronauts().stream(),
            planning.getMissionControlOfficers().stream()).toList()) {
            person.setWorkSeasons(6);
        }
        planning.getAstronauts().get(0).setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait
            .COOL_UNDER_PRESSURE)));
        planning.getAstronauts().get(3).setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait
            .MUST_BE_THE_BEST)));
        planning.getAstronauts().get(5).setWorkSeasons(1);
        planning.getMissionControlOfficers().get(1).setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait
            .TEAM_PLAYER)));
        planning.getMissionControlOfficers().get(4).setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait
            .LONER)));
        planning.getMissionControlOfficers().get(6).setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait
            .STEADY)));
        planning.setRapports(new ArrayList<>(List.of(
            new de.mkoehler.launchwindow.model.Rapport("a0", "a3", -48, 0, 9, true),
            new de.mkoehler.launchwindow.model.Rapport("a0", "a1", 40, 0, 8, true),
            new de.mkoehler.launchwindow.model.Rapport("c0", "c2", 55, 0, 11, true))));
        startSession(game, planning, info);
        write(folder, "people-assemble.png", new AssembleMissionScreen(game, "vostok-orbital"), null);
        write(folder, "people-assemble-picker.png", new AssembleMissionScreen(game, "vostok-orbital"),
            screen -> screen.openPicker(false, 3));

        // The turn report's people news: two traits, a clash and a friendship.
        startSession(game, state, info);
        TurnReport peopleReport = new TurnReport(TurnReport.VERSION, 13, 2_100, 2_140, 17_000, 18_420, List.of(),
            List.of(), List.of(
                new de.mkoehler.launchwindow.rules.TraitRevealed(PersonnelGroup.SET, "p3", set.get(2).getName(),
                    de.mkoehler.launchwindow.model.Trait.TEAM_PLAYER, "rd", "vostok-capsule", 4),
                new de.mkoehler.launchwindow.rules.TraitRevealed(PersonnelGroup.SET, "p4", set.get(3).getName(),
                    de.mkoehler.launchwindow.model.Trait.QUICK_STUDY, "rd", "sputnik-1-satellite", 3),
                new de.mkoehler.launchwindow.rules.RelationshipShown("p1", set.get(0).getName(), "p5",
                    set.get(4).getName(), false, "rd", "vostok-capsule", 7),
                new de.mkoehler.launchwindow.rules.RelationshipShown("p4", set.get(3).getName(), "p7",
                    set.get(6).getName(), true, "rd", "sputnik-1-satellite", 0)));
        game.session().keepReport(peopleReport);
        write(folder, "people-report.png", new TurnReportScreen(game, peopleReport), null);
    }

    /**
     * The People canvas's SET Centre: a Vostok capsule team with a friendship, a revealed
     * Perfectionist and a Team player; a Sputnik team with someone new; idle engineers with traits; a
     * clash; and a candidate pool with hints.
     */
    private static PlayerSnapshot peopleState() {
        PlayerSnapshot state = new PlayerSnapshot("Baltic Star Agency", Archetype.SOVIET, 18_420, 2_140, 3_000, 13);
        state.setDirectorName("A. Reyes");
        state.getBuildingLevels().put(Building.SET_CENTER, 3);
        state.getOpenedProgramIds().addAll(List.of("sputnik-1", "vostok"));
        double capsuleCap = maxRd("vostok-capsule");
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(capsuleCap - 1.2, 38.0));
        state.getMissionComponents().put("sputnik-1-satellite", new OwnedMissionComponent(62.4, 45.0));

        SetSpecialist veteran = specialist("p1", "", 37, 81, 118, 52, 61, 44, 78, 30);
        SetSpecialist perfectionist = specialist("p2", "", 48, 74, 124, 66, 40, 58, 71, 22);
        SetSpecialist teamPlayer = specialist("p3", "", 48, 69, 112, 38, 57, 35, 66, 41);
        SetSpecialist quick = specialist("p4", "", 28, 72, 86, 41, 63, 29, 37, 25);
        SetSpecialist abrasive = specialist("p5", "", 41, 52, 109, 63, 50, 41, 68, 20);
        SetSpecialist fresh = specialist("p6", "", 28, 76, 82, 24, 31, 18, 22, 15);
        SetSpecialist mazur = specialist("p7", "", 42, 63, 104, 57, 59, 46, 40, 33);
        SetSpecialist idle = specialist("p8", "", 54, 66, 97, 45, 55, 39, 58, 47);
        for (SetSpecialist engineer : List.of(veteran, perfectionist, teamPlayer)) {
            engineer.setAssignmentState(AssignmentState.ASSIGNED);
            engineer.setRdTargetComponentId("vostok-capsule");
        }
        veteran.setRdSlot(0);
        perfectionist.setRdSlot(1);
        teamPlayer.setRdSlot(2);
        for (SetSpecialist engineer : List.of(quick, mazur)) {
            engineer.setAssignmentState(AssignmentState.ASSIGNED);
            engineer.setRdTargetComponentId("sputnik-1-satellite");
        }
        quick.setRdSlot(0);
        mazur.setRdSlot(1);
        fresh.setAssignmentState(AssignmentState.IN_TRAINING);
        fresh.setTrainingKind(TrainingKind.BASIC);
        fresh.setTrainingSeasonsTotal(2);
        fresh.setTrainingSeasonsCompleted(1);
        fresh.setHint("keen-to-learn");
        veteran.setHint("driven");
        int[] seasons = {14, 12, 10, 3, 9, 0, 11, 6};
        List<SetSpecialist> all = List.of(veteran, perfectionist, teamPlayer, quick, abrasive, fresh, mazur, idle);
        for (int i = 0; i < all.size(); i++) {
            all.get(i).setWorkSeasons(seasons[i]);
        }
        veteran.setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait.WORKAHOLIC),
            revealed(de.mkoehler.launchwindow.model.Trait.MENTOR)));
        perfectionist.setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait.PERFECTIONIST)));
        teamPlayer.setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait.TEAM_PLAYER)));
        quick.setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait.QUICK_STUDY)));
        abrasive.setTraits(List.of(revealed(de.mkoehler.launchwindow.model.Trait.ABRASIVE),
            revealed(de.mkoehler.launchwindow.model.Trait.LONER)));
        state.getSetSpecialists().addAll(all);
        state.setRapports(new ArrayList<>(List.of(
            new de.mkoehler.launchwindow.model.Rapport("p1", "p2", 52, 0, 12, true),
            new de.mkoehler.launchwindow.model.Rapport("p1", "p5", -34, 0, 7, true),
            new de.mkoehler.launchwindow.model.Rapport("p4", "p7", 31, 0, 3, true))));

        List<SetSpecialist> pool = List.of(
            specialist("c1", "", 52, 64, 96, 48, 66, 39, 52, 30),
            specialist("c2", "", 61, 70, 122, 71, 44, 63, 58, 35),
            specialist("c3", "", 34, 58, 88, 44, 52, 36, 47, 40),
            specialist("c4", "", 28, 73, 74, 33, 28, 41, 26, 19));
        String[] hints = {"driven", "patient", "independent", "pleasant"};
        for (int i = 0; i < pool.size(); i++) {
            pool.get(i).setHint(hints[i]);
        }
        state.getSetSpecialistCandidates().addAll(pool);
        return withPortraits(state);
    }

    private static de.mkoehler.launchwindow.model.TraitState revealed(de.mkoehler.launchwindow.model.Trait trait) {
        return new de.mkoehler.launchwindow.model.TraitState(trait, 0, true);
    }

    /** Pins the first chemistry breakdown on the screen open, as a click on it does. */
    private static void pinFirstBreakdown(com.badlogic.gdx.scenes.scene2d.Group root) {
        java.util.ArrayDeque<com.badlogic.gdx.scenes.scene2d.Actor> todo = new java.util.ArrayDeque<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            com.badlogic.gdx.scenes.scene2d.Actor actor = todo.removeFirst();
            if (actor instanceof de.mkoehler.launchwindow.client.ui.ChemistryIndicator indicator
                && indicator.popover() != null) {
                indicator.popover().setPinned(true);
                return;
            }
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
                group.getChildren().forEach(todo::add);
            }
        }
        throw new IllegalStateException("no chemistry breakdown on screen");
    }

    private static SetSpecialist specialist(String id, String name, int age, int morale, double salary,
                                            double rockets, double probes, double humanRated, double crewed,
                                            double eva) {
        SetSpecialist specialist = new SetSpecialist(id, name, age, morale, 60 + (int) (rockets % 30));
        specialist.setSalary(salary);
        specialist.setSkill(SetSkill.ROCKETS, rockets);
        specialist.setSkill(SetSkill.SPACE_PROBES, probes);
        specialist.setSkill(SetSkill.HUMAN_RATED_ROCKETS, humanRated);
        specialist.setSkill(SetSkill.CREWED_SPACECRAFT, crewed);
        specialist.setSkill(SetSkill.EVA_SUITS, eva);
        specialist.setAssignmentState(AssignmentState.IDLE);
        return specialist;
    }

    private static double maxRd(String componentId) {
        return ContentPackLoader.loadFromClasspath("content/early-era", ScreenSnapshot.class.getClassLoader())
            .missionComponent(componentId).orElseThrow().maxRdReliability();
    }

    private static ConnectScreen filledConnect(LaunchWindowGame game) {
        ConnectScreen screen = new ConnectScreen(game);
        screen.loginField().input().setText("mario");
        screen.passwordField().input().setText("hunter2");
        return screen;
    }

    private static JoinScreen filledJoin(LaunchWindowGame game) {
        JoinScreen screen = new JoinScreen(game, CREDENTIALS);
        screen.selectArchetype(Archetype.NASA);
        screen.agencyNameField().input().setText("Orion Spaceworks");
        screen.directorNameField().input().setText("A. Reyes");
        return screen;
    }

    /**
     * Lays a screen out, puts it into its state after a first layout pass (so anything placed
     * relative to a laid-out widget, such as a history-note card, lands where it would on screen),
     * draws it into a frame buffer and writes it out.
     */
    /**
     * Pins open the last history note found under a widget, in drawing order.
     *
     * @param root the widget to search
     */
    private static void pinLastHistoryNote(com.badlogic.gdx.scenes.scene2d.Group root) {
        HistoryNote last = null;
        java.util.ArrayDeque<com.badlogic.gdx.scenes.scene2d.Actor> todo = new java.util.ArrayDeque<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            com.badlogic.gdx.scenes.scene2d.Actor actor = todo.removeFirst();
            if (actor instanceof HistoryNote note) {
                last = note;
            }
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
                group.getChildren().forEach(todo::add);
            }
        }
        if (last != null) {
            last.setPinned(true);
        }
    }

    /**
     * Shows the first tooltip under a widget whose title starts with the given text, as if the
     * pointer rested on its actor.
     *
     * @param root  the widget to search
     * @param title the tooltip title's start
     */
    /** Hovers the first concept chip under a root and presses the second, as a pointer would. */
    private static void pointAtChips(com.badlogic.gdx.scenes.scene2d.Group root) {
        java.util.List<de.mkoehler.launchwindow.client.ui.ConceptChip> chips = new java.util.ArrayList<>();
        java.util.ArrayDeque<com.badlogic.gdx.scenes.scene2d.Actor> todo = new java.util.ArrayDeque<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            com.badlogic.gdx.scenes.scene2d.Actor actor = todo.removeFirst();
            if (actor instanceof de.mkoehler.launchwindow.client.ui.ConceptChip chip) {
                chips.add(chip);
            } else if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
                group.getChildren().forEach(todo::add);
            }
        }
        for (int i = 0; i < Math.min(2, chips.size()); i++) {
            de.mkoehler.launchwindow.client.ui.ConceptChip chip = chips.get(i);
            for (com.badlogic.gdx.scenes.scene2d.EventListener listener : chip.getListeners()) {
                if (listener instanceof com.badlogic.gdx.scenes.scene2d.utils.ClickListener click) {
                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setStage(chip.getStage());
                    event.setListenerActor(chip);
                    click.enter(event, 1f, 1f, -1, null);
                    if (i == 1) {
                        event.setType(com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown);
                        click.touchDown(event, 1f, 1f, 0, 0);
                    }
                }
            }
            chip.act(0f);
        }
    }

    /** Scrolls every scroll pane under a group to its end, as a player scrolling down would. */
    private static void scrollToEnd(com.badlogic.gdx.scenes.scene2d.Group root) {
        root.getStage().draw();
        java.util.ArrayDeque<com.badlogic.gdx.scenes.scene2d.Actor> todo = new java.util.ArrayDeque<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            com.badlogic.gdx.scenes.scene2d.Actor actor = todo.removeFirst();
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.ui.ScrollPane pane) {
                pane.layout();
                pane.setScrollPercentY(1f);
                pane.updateVisualScroll();
            }
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
                todo.addAll(java.util.Arrays.asList(group.getChildren().toArray()));
            }
        }
    }

    private static void showTooltip(com.badlogic.gdx.scenes.scene2d.Group root, String title) {
        java.util.ArrayDeque<com.badlogic.gdx.scenes.scene2d.Actor> todo = new java.util.ArrayDeque<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            com.badlogic.gdx.scenes.scene2d.Actor actor = todo.removeFirst();
            for (com.badlogic.gdx.scenes.scene2d.EventListener listener : actor.getListeners()) {
                if (listener instanceof com.badlogic.gdx.scenes.scene2d.ui.Tooltip<?> tooltip
                    && tooltip.getActor() instanceof com.badlogic.gdx.scenes.scene2d.ui.Table card
                    && card.getChildren().first() instanceof com.badlogic.gdx.scenes.scene2d.ui.Label label
                    && label.getText().toString().startsWith(title)) {
                    tooltip.setInstant(true);
                    com.badlogic.gdx.scenes.scene2d.InputEvent event = new com.badlogic.gdx.scenes.scene2d.InputEvent();
                    event.setStage(actor.getStage());
                    event.setListenerActor(actor);
                    tooltip.enter(event, actor.getWidth() / 2f, actor.getHeight() / 2f, -1, null);
                    // Snapshots render with no time passing: finish the short fade-in at once.
                    tooltip.getContainer().act(1f);
                    return;
                }
            }
            if (actor instanceof com.badlogic.gdx.scenes.scene2d.Group group) {
                group.getChildren().forEach(todo::add);
            }
        }
        throw new IllegalStateException("no tooltip titled " + title);
    }

    private static <S extends Screen> void write(File folder, String name, S screen, Consumer<S> state) {
        screen.show();
        screen.resize(width, height);
        screen.render(0f);
        if (state != null) {
            state.accept(screen);
        }
        FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        buffer.begin();
        screen.render(0f);
        screen.render(0f);
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
        buffer.end();
        Pixmap upright = flipped(pixmap);
        PixmapIO.writePNG(Gdx.files.absolute(new File(folder, name).getAbsolutePath()), upright);
        checkTextFit(folder, name, screen, upright);
        upright.dispose();
        pixmap.dispose();
        buffer.dispose();
        screen.hide();
        screen.dispose();
    }

    /** Every shot's text that doesn't fit ({@link TextFit}), one line a problem, in the order shot. */
    private static final List<String> TEXT_FIT = new ArrayList<>();

    /**
     * Checks a shot's text with {@link TextFit}; with {@code -Dtextfit.marks=true} also writes the
     * shot with its problems outlined in red to {@code marked/}.
     */
    private static void checkTextFit(File folder, String name, Screen screen, Pixmap upright) {
        com.badlogic.gdx.scenes.scene2d.Stage stage;
        try {
            java.lang.reflect.Method method = screen.getClass().getMethod("stage");
            method.setAccessible(true); // declared on the package-private StageScreen
            stage = (com.badlogic.gdx.scenes.scene2d.Stage) method.invoke(screen);
        } catch (ReflectiveOperationException notAStageScreen) {
            return;
        }
        List<TextFit.Issue> issues = TextFit.check(stage);
        issues.forEach(issue -> TEXT_FIT.add(issue.line(name)));
        if (!issues.isEmpty() && Boolean.getBoolean("textfit.marks")) {
            TextFit.mark(upright, stage.getViewport().getWorldHeight(), issues);
            File marked = new File(folder, "marked");
            marked.mkdirs();
            PixmapIO.writePNG(Gdx.files.absolute(new File(marked, name).getAbsolutePath()), upright);
        }
    }

    /** Writes every shot's text problems to {@code text-fit.tsv} beside the shots. */
    private static void writeTextFit(File folder) {
        try {
            Files.write(folder.toPath().resolve("text-fit.tsv"), TEXT_FIT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a copy turned upside down: OpenGL reads rows bottom first.
     */
    private static Pixmap flipped(Pixmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        Pixmap result = new Pixmap(width, height, source.getFormat());
        ByteBuffer from = source.getPixels();
        ByteBuffer to = result.getPixels();
        int rowBytes = width * 4;
        byte[] row = new byte[rowBytes];
        for (int y = 0; y < height; y++) {
            from.position((height - 1 - y) * rowBytes);
            from.get(row);
            to.position(y * rowBytes);
            to.put(row);
        }
        from.position(0);
        to.position(0);
        return result;
    }
}
