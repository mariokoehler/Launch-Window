package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import de.mkoehler.launchwindow.client.LaunchWindowGame;
import de.mkoehler.launchwindow.client.connect.ClaimResult;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.GameInfo;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.ui.FormField;
import de.mkoehler.launchwindow.client.ui.HistoryNotes;
import de.mkoehler.launchwindow.client.ui.OptionCard;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.NameSuggestions;
import de.mkoehler.launchwindow.content.AgencyNames;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * The Join Game screen ({@code ui-spec.md} &sect;4.2), shown the first time an account
 * connects to a game: pick NASA-style or Soviet-style, name the agency and its director, join.
 * The Director field carries the first history note a player meets.
 *
 * @author Mario Koehler
 */
public final class JoinScreen extends StageScreen {

    private static final float COLUMN_WIDTH = 920f;
    /** The speech, from NASA's historical sounds (assets/CREDITS.md). */
    static final String SPEECH = "audio/moon-speech.mp3";
    private static final String TAKEN = "Already taken by another agency.";

    private final Credentials credentials;
    private final Map<Archetype, OptionCard> archetypeCards = new EnumMap<>(Archetype.class);
    private final Label archetypeError;
    private final Cell<Label> archetypeErrorCell;
    private final FormField agencyName;
    private final FormField directorName;
    private final TextButton join;
    private final TextButton randomize;
    private final NameSuggestions suggestions = new NameSuggestions(AgencyNames.standard(), new Random());
    private final Cell<Actor> bannerCell;
    private Archetype archetype;
    /** Kennedy's "We choose to go to the Moon", once, when the screen first shows (ui-spec.md 4.2). */
    private com.badlogic.gdx.audio.Music speech;
    private boolean spoken;

    /**
     * Builds the screen.
     *
     * @param game        the game
     * @param credentials the login that has no slot yet
     */
    public JoinScreen(LaunchWindowGame game, Credentials credentials) {
        super(game);
        this.credentials = credentials;

        Table card = ui.card();
        float inner = COLUMN_WIDTH - 2 * (Theme.SPACE_4 + Theme.BORDER_EMPHASIS);
        float half = (inner - Theme.SPACE_4) / 2f;
        Table archetypes = new Table();
        archetypes.add(archetypeCard(Archetype.NASA, "NASA-style", "Open program, public press.", half))
            .width(half).top().padRight(Theme.SPACE_4);
        archetypes.add(archetypeCard(Archetype.SOVIET, "Soviet-style", "State program, OKB bureaus.", half))
            .width(half).top();
        archetypeError = ui.paragraph("", Theme.TextStyle.CAPTION, Theme.DANGER);

        randomize = ui.button("Randomize", Theme.ButtonKind.SECONDARY);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                suggestName();
            }
        });
        ui.tooltip(randomize, "Suggest a name", "Fills in a name in the style of the agency you picked, or "
            + "either style if you haven't picked one. Press again for another: none comes up twice until all "
            + suggestions.size(Archetype.NASA) + " of a style have. Change it as you like.");
        agencyName = new FormField(ui, "Agency name", "", "e.g. Orion Spaceworks").reserveHistoryNoteSpace()
            .withAction(randomize);
        directorName = new FormField(ui, "Director's name", "", "e.g. A. Reyes")
            .withHistoryNote(HistoryNotes.DIRECTOR);
        for (FormField field : new FormField[] {agencyName, directorName}) {
            field.input().setTextFieldListener(this::submitOnEnter);
            field.input().setMaxLength(ClaimSlotRequest.MAX_NAME_LENGTH);
        }
        Table names = new Table();
        names.add(agencyName).width(half).top().padRight(Theme.SPACE_4);
        names.add(directorName).width(half).top();

        join = ui.button("Join game", Theme.ButtonKind.PRIMARY);
        join.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submit();
            }
        });

        card.defaults().growX().left();
        card.add(archetypes).row();
        archetypeErrorCell = card.add((Label) null);
        card.row();
        card.add(names).padTop(Theme.SPACE_4).row();
        card.add(join).padTop(Theme.SPACE_4);

        Table column = new Table();
        column.defaults().width(COLUMN_WIDTH);
        column.add(centered(ui.label("Join game", Theme.TextStyle.EYEBROW, Theme.ON_PAGE_MUTED)))
            .padBottom(Theme.SPACE_1).row();
        column.add(centered(ui.label("Found your agency", Theme.TextStyle.HEADING_LG, Theme.ON_PAGE_STRONG)))
            .padBottom(Theme.SPACE_5).row();
        column.add(ui.notice(Theme.NoticeKind.INFO, "Everyone starts from the same line.",
            "Whenever you join, your agency starts fresh: every building at level 1, empty rosters, and ten "
                + "candidates waiting in each hiring pool. Hire SET specialists first. They research the parts "
                + "every mission flies on.")).padBottom(Theme.SPACE_5).row();
        bannerCell = column.add((Actor) null);
        column.row();
        column.add(card).row();
        column.add(ui.paragraph("Agency name and director's name must be unique among current players "
            + "(not case-sensitive).", Theme.TextStyle.CAPTION, Theme.ON_PAGE_MUTED)).padTop(Theme.SPACE_5);
        root.add(column);
    }

    @Override
    public void show() {
        super.show();
        stage.setKeyboardFocus(agencyName.input());
        if (!spoken) {
            spoken = true;
            speech = LaunchWindowGame.playOnce(SPEECH);
        }
    }

    @Override
    public void dispose() {
        if (speech != null) {
            speech.dispose();
        }
        super.dispose();
    }

    /**
     * Returns the agency-name field.
     *
     * @return the field
     */
    public FormField agencyNameField() {
        return agencyName;
    }

    /**
     * Returns the director-name field.
     *
     * @return the field
     */
    public FormField directorNameField() {
        return directorName;
    }

    /**
     * Fills the agency name with the next suggestion for the archetype picked, as the Randomize
     * button does.
     */
    public void suggestName() {
        suggestions.next(archetype).ifPresent(name -> {
            agencyName.input().setText(name);
            agencyName.input().setCursorPosition(name.length());
            agencyName.setError(null);
            // Back to the box, so the name can be edited, or Enter joins, straight away.
            stage.setKeyboardFocus(agencyName.input());
        });
    }

    /**
     * Returns the Randomize button.
     *
     * @return the button
     */
    public TextButton randomizeButton() {
        return randomize;
    }

    /**
     * Selects an archetype, as clicking its card does.
     *
     * @param chosen the archetype
     */
    public void selectArchetype(Archetype chosen) {
        archetype = chosen;
        archetypeCards.forEach((kind, card) -> card.setSelected(kind == chosen));
        setArchetypeError(null);
    }

    /**
     * Checks the form and, if it's complete, claims the slot. Missing choices are shown at once.
     */
    public void submit() {
        if (join.isDisabled()) {
            return;
        }
        clearMessages();
        if (archetype == null) {
            setArchetypeError("Pick an agency style.");
        }
        if (agencyName.text().isEmpty()) {
            agencyName.setError("Required.");
        }
        if (directorName.text().isEmpty()) {
            directorName.setError("Required.");
        }
        if (archetype == null || !agencyName.error().isEmpty() || !directorName.error().isEmpty()) {
            return;
        }
        setBusy(true);
        game.gateway().claimSlot(credentials, archetype, agencyName.text(), directorName.text(), result -> {
            if (isShowing()) {
                applyClaimResult(result);
            }
        });
    }

    /**
     * Shows a claim's outcome: moves on to the agency, or shows the refusal where it belongs.
     *
     * @param result the outcome
     */
    public void applyClaimResult(ClaimResult result) {
        setBusy(false);
        switch (result.kind()) {
            case JOINED -> enter(result.state(), result.gameInfo());
            case REFUSED -> showRefusal(result);
            case LOGIN_FAILED -> showBanner("Couldn't log in again.", result.message());
            case UNREACHABLE -> showBanner("Can't reach the server.", "Check your connection and try again.");
            case FAILED -> showBanner("The connection broke off.",
                result.message().isEmpty() ? "Try again." : result.message());
        }
    }

    /**
     * Shows or ends the "joining" state: the button is disabled and says so.
     *
     * @param busy whether a claim is under way
     */
    public void setBusy(boolean busy) {
        join.setDisabled(busy);
        randomize.setDisabled(busy);
        join.setText(busy ? "Joining…" : "Join game");
    }

    private void showRefusal(ClaimResult result) {
        switch (result.reason()) {
            case AGENCY_NAME_TAKEN -> agencyName.setError(TAKEN);
            case DIRECTOR_NAME_TAKEN -> directorName.setError(TAKEN);
            case MISSING_FIELDS -> {
                agencyName.setError(agencyName.text().isEmpty() ? "Required." : null);
                directorName.setError(directorName.text().isEmpty() ? "Required." : null);
            }
            case ALREADY_CLAIMED -> enterExistingSlot();
            case NAME_TOO_LONG -> {
                String tooLong = "At most " + ClaimSlotRequest.MAX_NAME_LENGTH + " characters.";
                agencyName.setError(agencyName.text().strip().length() > ClaimSlotRequest.MAX_NAME_LENGTH ? tooLong : null);
                directorName.setError(directorName.text().strip().length() > ClaimSlotRequest.MAX_NAME_LENGTH ? tooLong : null);
            }
            default -> showBanner("The server couldn't create your agency.", result.message());
        }
    }

    /**
     * The account got a slot meanwhile (another client joined first): log in again and go to it.
     */
    private void enterExistingSlot() {
        setBusy(true);
        game.gateway().logIn(credentials, login -> {
            if (!isShowing()) {
                return;
            }
            setBusy(false);
            if (login.kind() == LoginResult.Kind.IN_GAME) {
                enter(login.state(), login.gameInfo());
            } else {
                showBanner("This account already has an agency here.", "Connect again to open it.");
            }
        });
    }

    private void enter(PlayerSnapshot state, GameInfo gameInfo) {
        String problem = game.enterGame(credentials, state, gameInfo);
        if (problem != null) {
            showBanner("Can't open this game.", problem);
        }
    }

    private static Label centered(Label label) {
        label.setAlignment(Align.center);
        return label;
    }

    private OptionCard archetypeCard(Archetype kind, String title, String description, float width) {
        OptionCard card = new OptionCard(ui, ui.archetypeArt(kind), title, description, width,
            () -> selectArchetype(kind));
        archetypeCards.put(kind, card);
        return card;
    }

    private void submitOnEnter(TextField field, char key) {
        if (key == '\r' || key == '\n') {
            submit();
        }
    }

    private void setArchetypeError(String message) {
        if (message == null) {
            archetypeErrorCell.clearActor();
            archetypeErrorCell.padTop(0f);
        } else {
            archetypeError.setText(message);
            archetypeErrorCell.setActor(archetypeError);
            archetypeErrorCell.padTop(Theme.SPACE_2);
        }
        root.invalidateHierarchy();
    }

    private void clearMessages() {
        setArchetypeError(null);
        agencyName.setError(null);
        directorName.setError(null);
        bannerCell.clearActor();
        bannerCell.padBottom(0f);
    }

    private void showBanner(String lead, String text) {
        bannerCell.setActor(ui.notice(Theme.NoticeKind.DANGER, lead, text));
        bannerCell.padBottom(Theme.SPACE_4);
        root.invalidateHierarchy();
    }
}
