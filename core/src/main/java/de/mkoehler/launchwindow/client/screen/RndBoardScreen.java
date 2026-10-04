package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.PayloadRnd;

import java.util.List;

/**
 * An R&amp;D board (component 10): the SET Centre's Payload R&amp;D ({@code ui-spec.md} &sect;4.8.3)
 * or the VAB's Rocket R&amp;D (&sect;4.5.3), which work the same way. One card per owned, active
 * component with its reliability bar and four R&amp;D slots. Assign an idle engineer (they take
 * the lowest free slot) or release one; a component at its R&amp;D ceiling shows "Cap reached" and
 * its slots released.
 *
 * @author Mario Koehler
 */
public final class RndBoardScreen extends ChromeScreen {

    /**
     * Which board: what it holds, where it sits, and what it says when empty.
     */
    public enum Board {
        /** Payload components, in the SET Centre. */
        PAYLOAD("SET Centre", "Payload R&D", "No payload components yet. Open a program in Headquarters to get its "
            + "satellites, probes and capsules; they show up here for R&D."),
        /** Rockets, in the VAB. */
        ROCKET("VAB", "Rocket R&D", "No rockets yet. Open a rocket program in the VAB; its rocket shows up here "
            + "for R&D.");

        private final String building;
        private final String title;
        private final String empty;

        Board(String building, String title, String empty) {
            this.building = building;
            this.title = title;
            this.empty = empty;
        }

        private List<Crumb> crumbs() {
            return this == PAYLOAD
                ? List.of(Crumb.SPACE_COMPLEX, Crumb.SET_CENTRE, new Crumb(title, null))
                : List.of(Crumb.SPACE_COMPLEX, Crumb.VAB, Crumb.ROCKET_PROGRAMS, new Crumb(title, null));
        }
    }

    private final Board board;
    private final RndCards cards;

    /**
     * Builds the screen from the session.
     *
     * @param game  the game, in a session
     * @param board which board
     */
    public RndBoardScreen(LaunchWindowGame game, Board board) {
        super(game, board.crumbs(), board.building, board.title);
        this.board = board;
        this.cards = new RndCards(this, board.title, true);
        rebuild();
    }

    @Override
    protected void goBack() {
        if (board == Board.PAYLOAD) {
            goSetCentre();
        } else {
            game.setScreen(new RocketProgramsScreen(game));
        }
    }

    @Override
    protected void rebuild() {
        content.clearChildren();
        content.top();
        List<PayloadRnd.Card> cards = cards();
        if (cards.isEmpty()) {
            Table empty = ui.card();
            empty.add(ui.paragraph(board.empty, Theme.TextStyle.BODY, Theme.INK_MUTED)).growX();
            content.add(empty).growX();
            return;
        }
        Table grid = new Table();
        grid.top();
        for (int i = 0; i < cards.size(); i++) {
            grid.add(card(cards.get(i))).expandX().fillX().uniformX().top().pad(0f, 0f, Theme.SPACE_4,
                i % 2 == 0 ? Theme.SPACE_4 : 0f);
            if (i % 2 == 1) {
                grid.row();
            }
        }
        if (cards.size() % 2 == 1) {
            grid.add().expandX().fillX().uniformX();
        }
        ScrollPane scroll = scrollPane(grid);
        content.add(scroll).expand().fill();
    }

    /**
     * Returns the board's cards.
     *
     * @return the cards
     */
    public List<PayloadRnd.Card> cards() {
        return PayloadRnd.cards(session.state(), session.catalog(), board == Board.ROCKET);
    }

    private Table card(PayloadRnd.Card card) {
        return cards.card(card);
    }

    /**
     * Opens the assign dialog for a component: the idle engineers, best in its skill first.
     *
     * @param card the component's card
     */
    public void openAssign(PayloadRnd.Card card) {
        cards.openAssign(card);
    }
}
