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
import de.mkoehler.launchwindow.client.screen.AgencyScreen;
import de.mkoehler.launchwindow.client.screen.ConnectScreen;
import de.mkoehler.launchwindow.client.screen.JoinScreen;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.screen.HireScreen;
import de.mkoehler.launchwindow.client.screen.PayloadRndScreen;
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
        write(folder, "agency.png", new AgencyScreen(game), null);

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
        write(folder, "payload-rnd.png", new PayloadRndScreen(game), null);
        write(folder, "payload-rnd-assign.png", new PayloadRndScreen(game), screen -> screen.openAssign(
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
        write(folder, "set-empty.png", new SetCentreScreen(game), null);
        write(folder, "payload-rnd-empty.png", new PayloadRndScreen(game), null);
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
