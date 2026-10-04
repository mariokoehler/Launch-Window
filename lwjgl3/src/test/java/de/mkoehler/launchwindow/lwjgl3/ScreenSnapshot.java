package de.mkoehler.launchwindow.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ClaimResult;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.client.connect.ServerGateway;
import de.mkoehler.launchwindow.client.screen.AgencyScreen;
import de.mkoehler.launchwindow.client.screen.ConnectScreen;
import de.mkoehler.launchwindow.client.screen.JoinScreen;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;

import java.io.File;
import java.nio.ByteBuffer;
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
            CREDENTIALS, new LoginResult(LoginResult.Kind.VERSION_MISMATCH, "", "0.4.0", "0.5.0", null)));
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
        write(folder, "agency.png", new AgencyScreen(game, CREDENTIALS, state), null);
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
