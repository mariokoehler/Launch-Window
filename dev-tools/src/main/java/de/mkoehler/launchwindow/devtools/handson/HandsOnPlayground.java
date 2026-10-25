package de.mkoehler.launchwindow.devtools.handson;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import de.mkoehler.launchwindow.actions.HandsOnAction;
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
import de.mkoehler.launchwindow.client.screen.HandsOnScreen;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.HandsOn;
import de.mkoehler.launchwindow.rules.RdDecisions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Plays any hands-on decision's puzzle ({@code ui-spec.md} component 26, type 11) on its own, without a
 * server or a game to wait in for the decision to come up: pick a level, the component it runs on, the
 * lead engineer's skill and a seed, and the client's own {@link HandsOnScreen} plays it, every step
 * applied here by the rules the server runs ({@link RdDecisions#handsOn}). The levels are read from
 * {@code assets/content/decisions.json} before every run, so a level edited a minute ago plays without a
 * restart. On the puzzle's screen, R plays the same seed again, N a new one, and Esc returns to the list.
 *
 * <p>Run it with {@code mvn -pl dev-tools compile exec:exec@handson} (core installed first), which runs it
 * from {@code assets/}; {@code start_handson.cmd} does both.</p>
 *
 * @author Mario Koehler
 */
public final class HandsOnPlayground extends LaunchWindowGame {

    /** The content pack the components come from. */
    static final String PACK = "standard";
    /** A rocket under R&amp;D, for the rocket levels. */
    static final String ROCKET = "r7-vostok";
    /** A payload under R&amp;D, for the payload levels. */
    static final String PAYLOAD = "vostok-capsule";

    private static final Credentials CREDENTIALS =
        new Credentials(new ServerAddress("localhost", 45825), "tester", "");
    private static final int TURN = 22;

    private final Path assets;
    private HandsOnChooser chooser;
    private Decisions decisions = Decisions.EMPTY;

    /**
     * What one run plays.
     *
     * @param decisionId  the hands-on decision
     * @param componentId the component it runs on
     * @param skill       the lead engineer's skill in the component's skill, 0 to 100
     * @param seed        the seed the rolls come from
     */
    record Run(String decisionId, String componentId, int skill, long seed) {

        Run withSeed(long other) {
            return new Run(decisionId, componentId, skill, other);
        }
    }

    private HandsOnPlayground(Path assets) {
        this.assets = assets;
    }

    /**
     * Opens the playground.
     *
     * @param args none
     */
    public static void main(String[] args) throws ReflectiveOperationException, java.io.IOException {
        if (HandsOnPlayground.class.getClassLoader().getResource("content/" + Decisions.FILE) != null) {
            launch();
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
        loader.loadClass(HandsOnPlayground.class.getName()).getMethod("main", String[].class).invoke(null,
            (Object) args);
    }

    private static void launch() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Launch Window · Hands-on playground");
        configuration.useVsync(true);
        configuration.setWindowedMode(1600, 900);
        new Lwjgl3Application(new HandsOnPlayground(findAssets()), configuration);
    }

    private static Path findAssets() {
        for (String candidate : List.of(".", "assets", "../assets")) {
            Path path = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.exists(path.resolve("content").resolve(Decisions.FILE))) {
                return path;
            }
        }
        throw new IllegalStateException("Run it from the repository or its assets folder: no "
            + "content/" + Decisions.FILE + " found.");
    }

    @Override
    protected ServerGateway createGateway() {
        return new LocalServer();
    }

    @Override
    public void create() {
        super.create();
        ClassLoader loader = HandsOnPlayground.class.getClassLoader();
        String hash = ContentPackHash.ofClasspath("content/" + PACK, loader);
        GameInfoResponse info = GameInfoResponse.success(PACK, hash, List.of(), TURN + 1, true, -1L,
            System.currentTimeMillis(), null, List.of(), List.of(), 0);
        PlayerSnapshot state = new PlayerSnapshot("Hands-on Test", Archetype.NASA, 100_000, 0, 0, TURN);
        state.setDirectorName("Tester");
        setSession(ClientSession.start(CREDENTIALS, state, new GameInfo(info, System.currentTimeMillis()), loader));
        reloadDecisions();
        chooser = new HandsOnChooser(this);
        setScreen(chooser);
        String smoke = System.getProperty("handson.smoke");
        if (smoke != null) {
            // Silent, but the sounds still load and play, so a missing file shows in the log.
            LaunchWindowGame.useSoundInMemory(true);
            LaunchWindowGame.setMusicVolume(0f);
            LaunchWindowGame.setEffectsVolume(0f);
            smokeTest(Path.of(smoke));
        }
    }

    /**
     * Reads the decisions again from {@code assets/}: a level may have changed since the last look.
     *
     * @return the decisions
     */
    Decisions reloadDecisions() {
        decisions = Decisions.load(assets.resolve("content").resolve(Decisions.FILE));
        // The screen and its view read the levels through the standard decisions.
        Decisions.setStandard(decisions);
        return decisions;
    }

    /**
     * Returns the content pack.
     *
     * @return the catalog
     */
    ContentCatalog catalog() {
        return session().catalog();
    }

    /**
     * Keeps the chooser for the whole run and never lets the puzzle lead into the game: any other screen
     * asked for (the R&amp;D board behind Back, a link in the chrome) goes back to the chooser.
     */
    @Override
    public void setScreen(Screen screen) {
        if (chooser != null && screen != chooser && !(screen instanceof HandsOnScreen)) {
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
     * Starts a run: a fresh agency with the component under R&amp;D and its lead, the decision offered
     * and its playing option taken, as the banner's Play would, then the puzzle's screen.
     *
     * @param run what to play
     */
    void play(Run run) {
        chooser.remember(run);
        String problem = start(run);
        if (problem != null) {
            chooser.problem(problem);
            setScreen(chooser);
            return;
        }
        HandsOnScreen screen = new HandsOnScreen(this);
        screen.stage().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.R) {
                    Gdx.app.postRunnable(() -> play(run));
                    return true;
                }
                if (keycode == Input.Keys.N) {
                    Gdx.app.postRunnable(() -> play(run.withSeed(HandsOnChooser.newSeed())));
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    Gdx.app.postRunnable(() -> setScreen(chooser));
                    return true;
                }
                return false;
            }
        });
        setScreen(screen);
    }

    /**
     * Sets the agency up for a run and starts its puzzle, with no screen.
     *
     * @param run what to play
     * @return what went wrong, or {@code null} when the puzzle started
     */
    private String start(Run run) {
        Decisions fresh = reloadDecisions();
        Decisions.Decision decision = fresh.byId(run.decisionId()).orElse(null);
        if (decision == null || decision.handsOn() == null || decision.playOption().isEmpty()) {
            return "No hands-on level " + run.decisionId() + " in decisions.json any more.";
        }
        PlayerSnapshot live = session().state();
        MissionComponent component = catalog().missionComponent(run.componentId()).orElseThrow();
        live.getMissionComponents().clear();
        live.getMissionComponents().put(component.id(), new OwnedMissionComponent(50.0, 3.0));
        live.getSetSpecialists().clear();
        // The rules want a team to size the effects from (NO_TEAM otherwise): the lead is it.
        live.getSetSpecialists().add(lead(component, run.skill()));
        live.setFunds(100_000);
        live.setPendingDecision(PendingDecision.offered(decision.id(), component.id(), TURN, run.seed()));
        RdDecisions.Outcome started = RdDecisions.decide(live, catalog(), fresh, decision.playOption().get().id(),
            null, null);
        return started == RdDecisions.Outcome.STARTED ? null
            : "The rules refused to start " + decision.id() + ": " + started + ".";
    }

    /** The team's lead: one engineer on the component, with the skill asked for in its skill. */
    private static SetSpecialist lead(MissionComponent component, int skill) {
        SetSpecialist lead = new SetSpecialist("lead", "Dana Whitfield", 41, 70, 70);
        for (SetSkill each : SetSkill.values()) {
            lead.setSkill(each, 30);
        }
        lead.setSkill(component.associatedSkill(), skill);
        lead.setAssignmentState(AssignmentState.ASSIGNED);
        lead.setRdTargetComponentId(component.id());
        lead.setRdSlot(0);
        return lead;
    }

    /**
     * A check that the puzzle plays, for {@code -Dhandson.smoke=<folder>}: finds a seed on which random
     * play meets the first level's objective, then plays that run on the screen step by step as clicks
     * would (through the gateway and back), saving the chooser, the first command queued, twelve frames of
     * the first round resolving up to a fail, three of the rest after the fail is accepted, the round's
     * results, the result and the chooser again, then exits.
     */
    private void smokeTest(Path folder) {
        Decisions.Decision first = decisions.decisions().stream().filter(d -> d.handsOn() != null).findFirst()
            .orElseThrow();
        String component = first.fits() == Decisions.Fits.PAYLOAD ? PAYLOAD : ROCKET;
        Run run = null;
        List<HandsOnAction> plan = List.of();
        for (long seed = 1; seed < 2_000 && run == null; seed++) {
            Run tried = new Run(first.id(), component, 60, seed);
            plan = randomPlay(tried, first.handsOn(), new java.util.SplittableRandom(seed));
            // A run that meets the objective, and whose first round stops at a fail to answer.
            if (puzzle().result() != HandsOnState.Result.FAILURE && firstRoundFails(plan)) {
                run = tried;
            }
        }
        Run winning = java.util.Objects.requireNonNull(run, "no seed met the objective");
        List<Runnable> shots = new java.util.ArrayList<>();
        shots.add(() -> grab(folder.resolve("smoke-chooser.png")));
        shots.add(() -> play(winning));
        boolean queuedOne = false;
        boolean roundShown = false;
        boolean confirmedOne = false;
        boolean answeredOne = false;
        RdDecisions.HandsOnStep previous = null;
        for (HandsOnAction action : plan) {
            if (action.getStep() == RdDecisions.HandsOnStep.QUEUE && previous != null
                    && previous != RdDecisions.HandsOnStep.QUEUE) {
                if (!roundShown) {
                    shots.add(() -> grab(folder.resolve("smoke-round.png")));
                    roundShown = true;
                }
                // The player's "Plan round n".
                shots.add(() -> screen().showRound(0));
            }
            shots.add(() -> screen().take(action.getStep(), action.getCommandId()));
            if (!queuedOne) {
                shots.add(() -> grab(folder.resolve("smoke-planning.png")));
                queuedOne = true;
            }
            if (action.getStep() == RdDecisions.HandsOnStep.CONFIRM && !confirmedOne) {
                // The first round resolves slot by slot up to its fail: frames of the needle, then time to finish.
                confirmedOne = true;
                waitAndGrab(shots, folder, "smoke-reveal-", 12, 30);
            } else if (action.getStep() == RdDecisions.HandsOnStep.ACCEPT && !answeredOne) {
                // The answer runs the rest of the round from the slot after the fail.
                answeredOne = true;
                waitAndGrab(shots, folder, "smoke-answer-", 3, 20);
            }
            previous = action.getStep();
        }
        shots.add(() -> grab(folder.resolve("smoke-finished.png")));
        shots.add(() -> setScreen(chooser));
        shots.add(() -> grab(folder.resolve("smoke-back.png")));
        shots.add(Gdx.app::exit);
        for (int i = 0; i < shots.size(); i++) {
            Runnable shot = shots.get(i);
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    shot.run();
                }
            }, 1.5f + i * 0.4f);
        }
    }

    /** Whether a plan's first confirm stops at a fail the player answers. */
    private static boolean firstRoundFails(List<HandsOnAction> plan) {
        for (int i = 0; i + 1 < plan.size(); i++) {
            if (plan.get(i).getStep() == RdDecisions.HandsOnStep.CONFIRM) {
                return plan.get(i + 1).getStep() == RdDecisions.HandsOnStep.ACCEPT;
            }
        }
        return false;
    }

    /** Waits {@code waits} shots of the smoke test, grabbing the first {@code frames} of them. */
    private static void waitAndGrab(List<Runnable> shots, Path folder, String prefix, int frames, int waits) {
        for (int wait = 1; wait <= waits; wait++) {
            int frame = wait;
            shots.add(frame <= frames ? () -> grab(folder.resolve(prefix + frame + ".png")) : () -> { });
        }
    }

    /** Plays a run out at random straight through the rules, no screen, and returns the steps taken. */
    private List<HandsOnAction> randomPlay(Run run, de.mkoehler.launchwindow.content.HandsOnLevel level,
                                           java.util.SplittableRandom random) {
        List<HandsOnAction> taken = new java.util.ArrayList<>();
        if (start(run) != null) {
            return taken;
        }
        for (int i = 0; i < 200 && inProgress(); i++) {
            HandsOnAction next;
            if (puzzle().phase() == HandsOnState.Phase.AWAITING_RESIST) {
                next = new HandsOnAction(RdDecisions.HandsOnStep.ACCEPT, null);
            } else {
                List<de.mkoehler.launchwindow.content.HandsOnLevel.Command> can = level.commands().stream()
                    .filter(command -> HandsOn.canQueue(level, puzzle(), command)).toList();
                next = puzzle().queue().size() < level.slots() && !can.isEmpty()
                    ? new HandsOnAction(RdDecisions.HandsOnStep.QUEUE, can.get(random.nextInt(can.size())).id())
                    : new HandsOnAction(RdDecisions.HandsOnStep.CONFIRM, null);
            }
            apply(next);
            taken.add(next);
        }
        return taken;
    }

    private HandsOnScreen screen() {
        return (HandsOnScreen) getScreen();
    }

    private HandsOnState puzzle() {
        return session().state().getPendingDecision().handsOn();
    }

    private boolean inProgress() {
        return session().state().getPendingDecision().handsOnInProgress();
    }

    private static void grab(Path file) {
        com.badlogic.gdx.graphics.Pixmap pixmap = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0, 0,
            Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.absolute(file.toAbsolutePath().toString()), pixmap,
            -1, true);
        pixmap.dispose();
    }

    /**
     * Applies a step of the puzzle to the agency, as the server would.
     *
     * @param action the action
     * @return what came of it
     */
    private ServerResult apply(PlayerAction action) {
        if (!(action instanceof HandsOnAction step)) {
            return ServerResult.of(ServerResult.Kind.REFUSED, "NOT_IN_PLAYGROUND");
        }
        PlayerSnapshot live = session().state();
        String outcome = RdDecisions.handsOn(live, catalog(), decisions, step.getStep(), step.getCommandId());
        return outcome.equals(HandsOn.Outcome.DONE.name())
            ? new ServerResult(ServerResult.Kind.OK, outcome, live, null)
            : ServerResult.of(ServerResult.Kind.REFUSED, outcome);
    }

    /**
     * The server, here: hands-on steps are applied at once by the rules, a refresh answers with the
     * state as it is, and nothing else does anything. Answers come on the next frame, as a real
     * server's would, never inside the click that asked.
     */
    private final class LocalServer implements ServerGateway {

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
            ServerResult result = apply(action);
            Gdx.app.postRunnable(() -> done.accept(result));
        }

        @Override
        public void refresh(Credentials credentials, Consumer<ServerResult> done) {
            ServerResult result = new ServerResult(ServerResult.Kind.OK, "", session().state(), null);
            Gdx.app.postRunnable(() -> done.accept(result));
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
