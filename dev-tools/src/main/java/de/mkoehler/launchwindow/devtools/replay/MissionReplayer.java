package de.mkoehler.launchwindow.devtools.replay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import de.mkoehler.launchwindow.client.ClientSession;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ClaimResult;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.connect.RivalsResult;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.client.connect.ServerGateway;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.client.connect.TurnReportResult;
import de.mkoehler.launchwindow.client.screen.ReplayScreen;
import de.mkoehler.launchwindow.client.view.ReplayClips;
import de.mkoehler.launchwindow.client.view.ReplaySounds;
import de.mkoehler.launchwindow.client.view.TurnReportView;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plays any mission season's replay with the variant asked for, without a server or a game to play
 * up to it: pick a mission, a season, a step (or all of them) and an outcome, and the client's own
 * replay screen plays a made-up report ({@link ReviewReport}). For reviewing the replay's clips
 * ({@code ui-spec.md} &sect;4.7.3). In the replay, keys 1 to 4 switch the outcome at the step
 * showing, R replays from the start, and Back or Esc returns to the list. The clips, their sidecars
 * and the sounds are read from {@code assets/} before every replay, so a clip rendered a minute ago
 * plays without a restart.
 *
 * <p>Run it with {@code mvn -pl dev-tools compile exec:exec@replayer} (core installed first), which
 * runs it from {@code assets/}; {@code -Dreplayer.pack=early-era} for the small pack.</p>
 *
 * @author Mario Koehler
 */
public final class MissionReplayer extends LaunchWindowGame {

    /** The outcomes on offer, in the keys' order (1 to 4). A saved glitch plays the glitch clip. */
    static final List<MissionStepOutcome> OUTCOMES = List.of(MissionStepOutcome.SUCCESSFUL,
        MissionStepOutcome.MINOR_GLITCH, MissionStepOutcome.FAILURE, MissionStepOutcome.CATASTROPHIC);

    private static final Credentials CREDENTIALS =
        new Credentials(new ServerAddress("localhost", 45825), "reviewer", "");

    private final String pack;
    private final Path assets;
    private ReplayChooser chooser;

    /**
     * What one replay plays.
     *
     * @param config  the mission
     * @param season  the season, from 0
     * @param outcome the outcome
     * @param target  the step that gets it, or -1 for every step
     */
    record Choice(MissionConfiguration config, int season, MissionStepOutcome outcome, int target) {

        Choice withOutcome(MissionStepOutcome other) {
            return new Choice(config, season, other, target);
        }
    }

    private MissionReplayer(String pack, Path assets) {
        this.pack = pack;
        this.assets = assets;
    }

    /**
     * Opens the replayer.
     *
     * @param args the content pack ({@code standard} if none)
     */
    public static void main(String[] args) throws ReflectiveOperationException, java.io.IOException {
        if (MissionReplayer.class.getClassLoader().getResource("content/" + ReplayClips.FILE) != null) {
            launch(args);
            return;
        }
        // The client finds its content, help and explainers on the classpath. Dev-tools' classpath
        // never has assets/ (the bots would play with its decisions), so start again on a loader
        // that has it, ahead of the rest.
        List<java.net.URL> urls = new java.util.ArrayList<>();
        urls.add(findAssets().toUri().toURL());
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            urls.add(Path.of(entry).toAbsolutePath().toUri().toURL());
        }
        java.net.URLClassLoader loader = new java.net.URLClassLoader(urls.toArray(java.net.URL[]::new),
            ClassLoader.getPlatformClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        loader.loadClass(MissionReplayer.class.getName()).getMethod("main", String[].class).invoke(null,
            (Object) args);
    }

    private static void launch(String[] args) {
        String pack = args.length > 0 && !args[0].isBlank() ? args[0] : "standard";
        Path assets = findAssets();
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Launch Window · Mission replayer");
        configuration.useVsync(true);
        configuration.setWindowedMode(1600, 900);
        new Lwjgl3Application(new MissionReplayer(pack, assets), configuration);
    }

    private static Path findAssets() {
        for (String candidate : List.of(".", "assets", "../assets")) {
            Path path = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.exists(path.resolve("content").resolve(ReplayClips.FILE))) {
                return path;
            }
        }
        throw new IllegalStateException("Run it from the repository or its assets folder: no "
            + "content/" + ReplayClips.FILE + " found.");
    }

    @Override
    protected ServerGateway createGateway() {
        return new NoServer();
    }

    @Override
    public void create() {
        super.create();
        ClassLoader loader = MissionReplayer.class.getClassLoader();
        String hash = ContentPackHash.ofClasspath("content/" + pack, loader);
        GameInfoResponse info = GameInfoResponse.success(pack, hash, List.of(), 41, true, -1L,
            System.currentTimeMillis(), null, List.of(), List.of(), 0);
        PlayerSnapshot state = new PlayerSnapshot("Replay Review", Archetype.NASA, 10_000, 0, 0, 40);
        state.setDirectorName("Reviewer");
        setSession(ClientSession.start(CREDENTIALS, state, new GameInfo(info, System.currentTimeMillis()), loader));
        chooser = new ReplayChooser(this, session().catalog(), assets);
        setScreen(chooser);
        String smoke = System.getProperty("replayer.smoke");
        if (smoke != null) {
            smokeTest(Path.of(smoke));
        }
    }

    /**
     * A check that the replay plays, for {@code -Dreplayer.smoke=<folder>}: the chooser, then the first
     * mission with clips (or {@code -Dreplayer.smoke.mission=<id>}), a success from step 0 (or
     * {@code -Dreplayer.smoke.step=<n>}) and a failure two steps on, a frame of each saved there, then exit.
     */
    private void smokeTest(Path folder) {
        String wanted = System.getProperty("replayer.smoke.mission");
        MissionConfiguration config = wanted != null ? session().catalog().missionConfiguration(wanted).orElseThrow()
            : session().catalog().missionConfigurations().stream()
            .filter(c -> c.seasons().get(0).steps().stream().anyMatch(s -> ReplayClips.standard().clipName(s.id())
                .isPresent())).findFirst().orElseThrow();
        int at = Integer.getInteger("replayer.smoke.step", 0);
        Runnable[] shots = {
            () -> grab(folder.resolve("smoke-chooser.png")),
            () -> play(new Choice(config, 0, MissionStepOutcome.SUCCESSFUL, -1), at),
            () -> grab(folder.resolve("smoke-success.png")),
            () -> play(new Choice(config, 0, MissionStepOutcome.FAILURE, at + 2), at + 2),
            () -> grab(folder.resolve("smoke-failure.png")),
            () -> play(new Choice(config, 0, MissionStepOutcome.FAILURE, 1), 5),
            () -> grab(folder.resolve("smoke-past-end.png")),
            () -> {
                setScreen(chooser);
                chooser.pick(config.id(), 0, 2);
            },
            () -> grab(folder.resolve("smoke-back.png")),
            Gdx.app::exit};
        for (int i = 0; i < shots.length; i++) {
            Runnable shot = shots[i];
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    shot.run();
                }
            }, 1.5f + i * 2.5f);
        }
    }

    private static void grab(Path file) {
        com.badlogic.gdx.graphics.Pixmap pixmap = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap,
            -1, true);
        pixmap.dispose();
    }

    /**
     * Keeps the chooser for the whole run and never lets the replay lead into the game: any other
     * screen asked for (a link in the chrome, say) goes back to the chooser.
     */
    @Override
    public void setScreen(Screen screen) {
        if (chooser != null && screen != chooser && !(screen instanceof ReplayScreen)) {
            Gdx.app.postRunnable(screen::dispose);
            screen = chooser;
        }
        if (chooser != null && getScreen() == chooser) {
            // Game.setScreen would dispose the chooser on the way out; keep it.
            chooser.hide();
            this.screen = screen;
            screen.show();
            screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else {
            super.setScreen(screen);
        }
    }

    /**
     * Plays a choice, reading the clips and sounds fresh from {@code assets/}.
     *
     * @param choice what to play
     * @param step   the step to start at, from 0
     */
    void play(Choice choice, int step) {
        chooser.remember(choice);
        ReplayClips.setStandard(ReplayClips.load(assets));
        ReplaySounds.setStandard(ReplaySounds.load(assets));
        TurnReport report = ReviewReport.build(session().catalog(), choice.config(), choice.season(),
            choice.outcome(), choice.target());
        ReplayScreen replay = new ReplayScreen(this, report, ReviewReport.MISSION_ID);
        replay.onLeave(() -> setScreen(chooser));
        replay.stage().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                int index = keycode - Input.Keys.NUM_1;
                if (index >= 0 && index < OUTCOMES.size()) {
                    Choice next = choice.withOutcome(OUTCOMES.get(index));
                    Gdx.app.postRunnable(() -> play(next, replay.step()));
                    return true;
                }
                if (keycode == Input.Keys.R) {
                    Gdx.app.postRunnable(() -> play(choice, 0));
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.app.postRunnable(() -> setScreen(chooser));
                    return true;
                }
                return false;
            }
        });
        setScreen(replay);
        // A failure on an earlier step ends the season sooner: past its end is the summary.
        int last = TurnReportView.steps(report, ReviewReport.MISSION_ID).size();
        if (step > 0) {
            replay.show(Math.min(step, last));
        }
    }

    /** No server: nothing is ever sent, nothing ever comes back. */
    private static final class NoServer implements ServerGateway {

        @Override
        public boolean isWaiting() {
            return false;
        }

        @Override
        public void logIn(Credentials credentials, Consumer<LoginResult> done) {
        }

        @Override
        public void claimSlot(Credentials credentials, Archetype archetype, String agencyName, String directorName,
                              Consumer<ClaimResult> done) {
        }

        @Override
        public void submit(Credentials credentials, PlayerAction action, long gameTurn, Consumer<ServerResult> done) {
        }

        @Override
        public void refresh(Credentials credentials, Consumer<ServerResult> done) {
        }

        @Override
        public void fetchTurnReport(Credentials credentials, long turn, Consumer<TurnReportResult> done) {
        }

        @Override
        public void resolveTurnNow(Credentials credentials, Consumer<ServerResult> done) {
        }

        @Override
        public void fetchRivals(Credentials credentials, Consumer<RivalsResult> done) {
        }

        @Override
        public void logOut() {
        }

        @Override
        public void dispose() {
        }
    }
}
