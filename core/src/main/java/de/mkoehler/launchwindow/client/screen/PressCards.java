package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.FrontPageView;
import de.mkoehler.launchwindow.client.view.MemorialView;
import de.mkoehler.launchwindow.content.MemorialPages;
import de.mkoehler.launchwindow.model.FrontPage;

import java.util.List;
import java.util.Optional;

/**
 * The cards that ask for a statement on a lost crew in the turn after the loss ({@code ui-spec.md} &sect;4.18.3,
 * component 29, from the canvas "Launch Window - In Memory"): the losing agency's "The press is waiting for your
 * statement", in its report and on the hub, and every other agency's news row with "Send condolences".
 *
 * @author Mario Koehler
 */
final class PressCards {

    private PressCards() {
    }

    /**
     * Returns this agency's own loss the press waits on, if it lost a crew in the turn before the open one.
     *
     * @param host the screen
     * @return the loss, if there is one
     */
    static Optional<FrontPage.Loss> waiting(SessionScreen host) {
        return host.session.ownLoss().filter(loss -> loss.gameTurn() + 1 == host.session.openTurn());
    }

    /**
     * Returns the other agencies' losses this agency may send condolences on, in the open turn.
     *
     * @param host the screen
     * @return the losses, maybe none
     */
    static List<FrontPage.Loss> others(SessionScreen host) {
        String mine = host.session.state().getAgencyName();
        return host.session.losses().stream()
            .filter(loss -> !loss.agencyName().equals(mine) && loss.gameTurn() + 1 == host.session.openTurn()).toList();
    }

    /**
     * The losing agency's card: the crew, what the paper will print, and the statement's button.
     *
     * @param host    the screen it's on
     * @param loss    the loss
     * @param hub     whether it's the hub's narrower card
     * @param changed what runs after a statement is given
     * @return the card
     */
    static Table own(SessionScreen host, FrontPage.Loss loss, boolean hub, Runnable changed) {
        Table card = new Table();
        card.setBackground(host.ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(Theme.SPACE_4, Theme.SPACE_5, Theme.SPACE_4, Theme.SPACE_5);
        Table words = new Table();
        words.left();
        words.defaults().left().growX();
        words.add(host.ui.label("THE PRESS", Theme.TextStyle.EYEBROW, Theme.NAVY)).padBottom(4f).row();
        words.add(host.ui.paragraph("The press is waiting for your statement", Theme.TextStyle.HEADING_SM, Theme.INK))
            .padBottom(Theme.SPACE_2).row();
        Table crew = new Table();
        crew.left();
        for (FrontPage.Lost lost : loss.crew()) {
            crew.add(portrait(host, lost, 40f)).size(40f).padRight(6f);
        }
        String names = MemorialView.crew(loss);
        String during = MemorialPages.standard().place(loss.stepId()).during();
        String line = hub ? names + ". Their memorial page is printed next season."
            : names + (loss.crew().size() == 1 ? " was lost " : " were lost ") + during
                + ". The paper prints their memorial page next season, with your agency’s statement.";
        crew.add(host.ui.paragraph(line, Theme.TextStyle.BODY_SM, Theme.INK)).growX().padLeft(Theme.SPACE_2);
        words.add(crew).padBottom(4f).row();
        if (!hub) {
            words.add(host.ui.paragraph("If you give none, a spokesman’s short one is printed.",
                Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        }
        Optional<FrontPage.Statement> given = host.session.statement(loss.id());
        TextButton button = host.ui.button(given.map(s -> "Your statement: " + FrontPageView.stanceTitle(s.stance())
            + " · Change").orElse("Give a statement"), given.isPresent() ? Theme.ButtonKind.SECONDARY
            : Theme.ButtonKind.PRIMARY);
        button.addListener(SessionScreen.clicked(() -> StatementDialog.eulogy(host, loss, changed).show()));
        if (hub) {
            card.add(words).growX().row();
            Table foot = new Table();
            foot.add(host.ui.label("Until the turn resolves", Theme.TextStyle.CAPTION, Theme.INK_MUTED)).left()
                .expandX();
            foot.add(button).right();
            card.add(foot).growX().padTop(Theme.SPACE_3);
        } else {
            card.add(words).expandX().fillX().left().padRight(Theme.SPACE_5);
            card.add(button).right().bottom();
        }
        return card;
    }

    /**
     * Another agency's loss, first in the news: what happened, and "Send condolences".
     *
     * @param host    the screen it's on
     * @param loss    the loss
     * @param changed what runs after condolences are sent or withdrawn
     * @return the row
     */
    static Table rival(SessionScreen host, FrontPage.Loss loss, Runnable changed) {
        Table row = new Table();
        row.left();
        Table words = new Table();
        words.left();
        words.defaults().left().growX();
        words.add(host.ui.paragraph(loss.agencyName() + " has lost the crew of "
            + MemorialView.flight(loss, host.session.catalog()), Theme.TextStyle.BODY_STRONG, Theme.INK))
            .padBottom(2f).row();
        String during = MemorialPages.standard().place(loss.stepId()).during();
        words.add(host.ui.paragraph(MemorialView.crew(loss) + " died " + during
            + ". Their memorial page is printed next season.", Theme.TextStyle.CAPTION, Theme.INK_MUTED));
        row.add(words).expandX().fillX().left().padRight(Theme.SPACE_3);
        Optional<FrontPage.Statement> sent = host.session.statement(loss.id());
        TextButton button = host.ui.button(sent.map(s -> "Sent: " + FrontPageView.stanceTitle(s.stance()))
            .orElse("Send condolences"), Theme.ButtonKind.SECONDARY);
        button.addListener(SessionScreen.clicked(() -> StatementDialog.condolences(host, loss, changed).show()));
        row.add(button).right().top();
        return row;
    }

    private static Image portrait(SessionScreen host, FrontPage.Lost lost, float size) {
        TextureRegionDrawable picture = lost.portraitId() == null ? null
            : host.ui.greyPicture("portraits/" + lost.portraitId() + ".jpg");
        return picture == null ? new Image(host.ui.solid(new Color(Theme.INK).mul(1f, 1f, 1f, 0.15f)))
            : new Image(picture, Scaling.fill);
    }
}
