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
import de.mkoehler.launchwindow.client.screen.JoinScreen;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.screen.BodyProgramsScreen;
import de.mkoehler.launchwindow.client.screen.HeadquartersScreen;
import de.mkoehler.launchwindow.client.screen.OpenProgramsScreen;
import de.mkoehler.launchwindow.client.screen.ProgramDetailsScreen;
import de.mkoehler.launchwindow.client.screen.SelectBodyScreen;
import de.mkoehler.launchwindow.client.view.Programs;
import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.client.screen.HireScreen;
import de.mkoehler.launchwindow.client.screen.SpaceComplexScreen;
import de.mkoehler.launchwindow.client.view.Hub;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.client.screen.RndBoardScreen;
import de.mkoehler.launchwindow.client.screen.OpenRocketScreen;
import de.mkoehler.launchwindow.client.screen.RocketProgramsScreen;
import de.mkoehler.launchwindow.client.screen.VabScreen;
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
import de.mkoehler.launchwindow.rules.MissionLaunch;
import de.mkoehler.launchwindow.rules.MissionStaffing;
import java.util.Arrays;
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
            public void create() {
                super.create();
                renderAll(this, folder);
                Gdx.app.exit();
            }
        }, configuration);
    }

    private static void renderAll(LaunchWindowGame game, File folder) {
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
        write(folder, "set-training-basic.png", new SetCentreScreen(game), screen -> screen.openTraining(
            game.session().state().getSetSpecialists().get(3), TrainingKind.BASIC, null));
        write(folder, "set-confirm-dismiss.png", new SetCentreScreen(game), screen -> screen.confirmDismiss(
            game.session().state().getSetSpecialists().get(2)));
        write(folder, "set-manage-department.png", new SetCentreScreen(game),
            screen -> screen.openManageDepartment(-10));
        write(folder, "set-toast.png", new SetCentreScreen(game), screen -> screen.toast(
            "Started training \u2014 Elena Markova (Permanent, Space probes).", Theme.NoticeKind.SUCCESS));
        write(folder, "payload-rnd.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), null);
        write(folder, "payload-rnd-assign.png", new RndBoardScreen(game, RndBoardScreen.Board.PAYLOAD), screen -> screen.openAssign(
            PayloadRnd.cards(game.session().state(), game.session().catalog()).get(2)));
        write(folder, "hire.png", new HireScreen(game), screen -> {
            List<SetSpecialist> pool = game.session().state().getSetSpecialistCandidates();
            screen.toggle(pool.get(1).getId());
            screen.toggle(pool.get(4).getId());
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
        return state;
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
