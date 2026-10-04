package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.Locale;

/**
 * A stand-in for the Space Complex hub ({@code ui-spec.md} &sect;4.3), which has no mockup yet:
 * proves the Connect and Join Game screens hand over a real logged-in state by showing the
 * agency, its director and its numbers, with a way back to Connect.
 *
 * @author Mario Koehler
 */
public final class AgencyScreen extends StageScreen {

    private static final float COLUMN_WIDTH = 480f;

    /**
     * Builds the screen.
     *
     * @param game        the game
     * @param credentials the login in use (kept for the hub that will replace this screen)
     * @param state       the player's state with today's actions applied
     */
    public AgencyScreen(LaunchWindowGame game, Credentials credentials, PlayerSnapshot state) {
        super(game);
        TextButton logOut = ui.button("Log out", Theme.ButtonKind.SECONDARY);
        logOut.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ConnectScreen(game));
            }
        });

        Table card = ui.card();
        card.defaults().growX().left().padBottom(Theme.SPACE_2);
        card.add(ui.label("Your agency", Theme.TextStyle.EYEBROW, Theme.NAVY)).row();
        card.add(ui.paragraph(state.getAgencyName(), Theme.TextStyle.HEADING_LG, Theme.INK)).row();
        card.add(ui.paragraph("Director " + state.getDirectorName() + " · " + archetypeName(state.getArchetype()),
            Theme.TextStyle.BODY, Theme.INK)).row();
        card.add(ui.paragraph(String.format(Locale.ROOT, "Funds %,d · Prestige %,d · Season %d",
            state.getFunds(), state.getPrestige(), state.getSeason()), Theme.TextStyle.BODY, Theme.INK)).row();
        card.add(ui.paragraph("The Space Complex isn't built yet. This page stands in for it.",
            Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(Theme.SPACE_4).row();
        card.add(logOut).left().expand(false, false).fill(false, false).padBottom(0f);
        root.add(card).width(COLUMN_WIDTH);
    }

    private static String archetypeName(Archetype archetype) {
        return archetype == Archetype.NASA ? "NASA-style" : "Soviet-style";
    }
}
