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
import de.mkoehler.launchwindow.client.view.Explainers;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.client.screen.RndBoardScreen;
import de.mkoehler.launchwindow.client.screen.OpenRocketScreen;
import de.mkoehler.launchwindow.client.screen.RocketProgramsScreen;
import de.mkoehler.launchwindow.client.screen.VabScreen;
import de.mkoehler.launchwindow.client.screen.MuseumScreen;
import de.mkoehler.launchwindow.client.screen.AstronautComplexScreen;
import de.mkoehler.launchwindow.client.screen.PublicAffairsScreen;
import de.mkoehler.launchwindow.model.NewsEntry;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.GoalRecord;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
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
        public void submit(Credentials credentials, PlayerAction action, Consumer<ServerResult> done) {
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
        new Lwjgl3Application(new LaunchWindowGame() {
            @Override
            protected ServerGateway createGateway() {
                return new SilentGateway();
            }

            @Override
            protected SettingsStore createSettingsStore() {
                // Never the player's real file: the shots always start from the defaults.
                return new SettingsStore(folder.toPath().resolve("no-such-folder").resolve("client-settings.json"));
            }

            @Override
            public void create() {
                super.create();
                renderAll(this, folder);
                Gdx.app.exit();
            }
        }, configuration);
    }

    private static void renderAll(LaunchWindowGame game, File folder) {
        // Explainers' read marks in memory, never this machine's: every explainer starts unread.
        game.useExplainerReadMarksInMemory(new java.util.HashSet<>());
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
        // The longest history note in the pack (the Sputnik 1 satellite's), pinned open.
        write(folder, "hq-details-sputnik-1-note.png", new ProgramDetailsScreen(game, "sputnik-1", null),
            screen -> pinLastHistoryNote(screen.stage().getRoot()));
        write(folder, "hq-details-sputnik-2.png", new ProgramDetailsScreen(game, "sputnik-2", CelestialBody.EARTH), null);

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
            new PlayerSnapshotStore(playerRoot, mapper).save(0, missionControlState(), null);
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
        String vostok = report.missions().stream().filter(flown -> flown.missionConfigurationId()
            .equals("vostok-orbital")).findFirst().orElseThrow().missionInstanceId();
        write(folder, "replay-step.png", new ReplayScreen(game, report, vostok), screen -> screen.show(
            firstInteresting(report, vostok)));
        write(folder, "replay-summary.png", new ReplayScreen(game, report, vostok), screen -> screen.show(99));
        write(folder, "replay-sputnik.png", new ReplayScreen(game, report, "m-sputnik"), screen -> screen.show(0));
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
        state.setNewsLog(List.of(new NewsEntry(17, RandomEventKind.MORALE_BOOST, null, 12),
            new NewsEntry(20, RandomEventKind.EMPLOYEE_UNAVAILABLE, null, 1),
            new NewsEntry(22, RandomEventKind.UNEXPECTED_COST, null, 1_249)));
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
        for (Explainers.Explainer explainer : Explainers.standard().explainers()) {
            for (int sheet = 0; sheet < explainer.sheets().size(); sheet++) {
                write(folder, "explainer-" + explainer.code().toLowerCase(java.util.Locale.ROOT) + "-" + (sheet + 1)
                    + ".png", new ExplainerScreen(game, explainer, sheet, () -> new MuseumScreen(game,
                    MuseumScreen.Tab.ENCYCLOPEDIA)), null);
            }
        }

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
        upright.dispose();
        pixmap.dispose();
        buffer.dispose();
        screen.hide();
        screen.dispose();
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
