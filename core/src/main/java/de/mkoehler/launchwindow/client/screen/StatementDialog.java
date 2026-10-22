package de.mkoehler.launchwindow.client.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import de.mkoehler.launchwindow.client.ui.HistoryNote;
import de.mkoehler.launchwindow.client.ui.Refusals;
import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.ui.UiKit;
import de.mkoehler.launchwindow.client.view.ContentNames;
import de.mkoehler.launchwindow.client.view.FrontPageView;
import de.mkoehler.launchwindow.client.view.MemorialView;
import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.MemorialPages;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.FrontPage;

import java.util.List;
import java.util.Optional;

/**
 * The statement dialog ({@code ui-spec.md} component 28, &sect;4.18.1): a rival's statement to the press
 * about a First, printed with every other rival's in the next turn's follow-up page. Five stance rows, an
 * optional quote with its counter and error, a preview of how it might read, and Submit, Cancel and, for a
 * statement already given, Withdraw. From the canvas "Launch Window - The World Reacts" (2026-10-22).
 *
 * <p>The same dialog, quieter, gives a lost crew's agency's statement (component 29, {@link #eulogy}) and a
 * rival's condolences ({@link #condolences}), from the canvas "Launch Window - In Memory" (2026-10-22).</p>
 *
 * @author Mario Koehler
 */
public final class StatementDialog {

    /**
     * What the dialog asks for: a First's statement, a lost crew's agency's statement or a rival's condolences.
     *
     * @param pageId       what the statement answers: a First's page or a loss
     * @param choices      the rows
     * @param title        the title
     * @param subtitle     the line under it
     * @param note         the history note after the title, maybe blank
     * @param choicesLabel the label over the rows
     * @param fieldLabel   the label over the words
     * @param placeholder  the words' placeholder
     * @param emptyPreview the preview before a row is picked
     * @param previewDate  the preview's right-hand line
     * @param footnote     the muted footnote
     * @param submitLabel  the primary button
     * @param givenPrefix  the status before the given choice's name ("Given this season")
     * @param quiet        whether the dialog keeps to navy and ink (no success green)
     * @param withdraw     whether a given statement can be withdrawn
     * @param crew         the lost crew shown over the rows, maybe none
     * @param preview      how a choice and words might read
     */
    private record Form(String pageId, List<FrontPage.Stance> choices, String title, String subtitle, String note,
                        String choicesLabel, String fieldLabel, String placeholder, String emptyPreview,
                        String previewDate, String footnote, String submitLabel, String givenPrefix, boolean quiet,
                        boolean withdraw, List<FrontPage.Lost> crew,
                        java.util.function.BiFunction<FrontPage.Stance, String, String> preview) {
    }

    /** The dialog's width. */
    static final float WIDTH = 720f;
    /** The quote box's height. */
    private static final float QUOTE_HEIGHT = 76f;
    private static final List<FrontPage.Stance> STANCES = List.of(FrontPage.Stance.ENTHUSIASTIC,
        FrontPage.Stance.POLITE, FrontPage.Stance.NEXT, FrontPage.Stance.DOWNPLAY, FrontPage.Stance.NO_COMMENT);

    private final SessionScreen host;
    private final UiKit ui;
    private final Form form;
    private final Runnable changed;
    private final Optional<FrontPage.Statement> given;
    private FrontPage.Stance stance;
    private String quote;
    private TextArea field;
    private Label counter;
    private Label error;
    private Label preview;
    private TextButton submit;

    /**
     * Prepares the dialog for a First's page, opened on the statement already given, if any.
     *
     * @param host    the screen it shows over
     * @param first   the First's page
     * @param changed what runs after a statement is kept or withdrawn (the strip's button changes)
     */
    public StatementDialog(SessionScreen host, FrontPage first, Runnable changed) {
        this(host, new Form(first.id(), STANCES, "Give a statement", subtitle(first),
            FrontPageReactions.standard().historyNotes().statement(), "YOUR STANCE", "QUOTE (OPTIONAL)",
            "Add a quote, printed word for word (optional)", "Pick a stance to see how it might read.",
            "The Daily Zenith, next season", "Every rival’s statement is printed together next season. You can change "
                + "or withdraw yours until the turn resolves.", "Submit statement", "Given this season", false, true,
            List.of(), (stance, quote) -> FrontPageView.sample(first, stance, quote,
                host.session.state().getAgencyName(), host.session.state().getDirectorName(),
                FrontPageReactions.standard(), ContentNames::of)), changed);
    }

    private StatementDialog(SessionScreen host, Form form, Runnable changed) {
        this.host = host;
        this.ui = host.ui;
        this.form = form;
        this.changed = changed;
        this.given = host.session.statement(form.pageId());
        this.stance = given.map(FrontPage.Statement::stance).orElse(null);
        this.quote = given.map(FrontPage.Statement::quote).orElse("");
    }

    /**
     * The statement of an agency that lost a crew, in one of four tones (component 29, &sect;4.18.3).
     *
     * @param host    the screen it shows over
     * @param loss    the loss
     * @param changed what runs after a statement is given
     * @return the dialog
     */
    public static StatementDialog eulogy(SessionScreen host, FrontPage.Loss loss, Runnable changed) {
        MemorialPages words = MemorialPages.standard();
        String season = GameDate.ofTurn(host.session.openTurn()).label();
        return new StatementDialog(host, new Form(loss.id(), List.of(FrontPage.Stance.COURAGE, FrontPage.Stance.PEOPLE,
            FrontPage.Stance.RESOLVE, FrontPage.Stance.CAUSE), "A statement on the loss of "
                + MemorialView.flight(loss, host.session.catalog()), "Printed in tomorrow’s paper, " + season,
            words.historyNotes().statement(), "THE TONE OF YOUR STATEMENT", "YOUR WORDS (OPTIONAL)",
            "Add words of your own, printed as you write them (optional)", "Pick a tone to see how it might read.",
            "The Daily Zenith, " + season, "If you give no statement, a spokesman’s short one is printed. You can "
                + "change yours until the turn resolves.", "Give statement", "Given this season", true, false,
            loss.crew(), (tone, quote) -> MemorialView.sampleStatement(loss, tone, quote, words)), changed);
    }

    /**
     * A rival's condolences on another agency's lost crew (component 28's condolence form, &sect;4.18.3).
     *
     * @param host    the screen it shows over
     * @param loss    the loss
     * @param changed what runs after condolences are sent or withdrawn
     * @return the dialog
     */
    public static StatementDialog condolences(SessionScreen host, FrontPage.Loss loss, Runnable changed) {
        MemorialPages words = MemorialPages.standard();
        String season = GameDate.ofTurn(host.session.openTurn()).label();
        return new StatementDialog(host, new Form(loss.id(), List.of(FrontPage.Stance.CONDOLENCES,
            FrontPage.Stance.TRIBUTE), "Condolences to " + loss.agencyName(), "On the loss of "
                + MemorialView.crew(loss)
                + " · printed next season", words.historyNotes().condolences(), "WHAT YOU SEND",
            "YOUR WORDS (OPTIONAL)", "Add words of your own, printed as you write them (optional)",
            "Pick one to see how it might read.", "The Daily Zenith, " + season,
            "Only what is sent is printed; silence is never reported.", "Send", "Sent this season", true, true,
            List.of(), (stance, quote) -> MemorialView.sampleCondolence(loss, stance, quote,
                host.session.state().getAgencyName(), host.session.state().getDirectorName(), words)), changed);
    }

    /**
     * Sets the stance and quote the dialog opens on, as if picked and typed ({@code ScreenSnapshot}).
     *
     * @param picked the stance, or {@code null}
     * @param typed  the quote
     * @return this dialog
     */
    public StatementDialog preset(FrontPage.Stance picked, String typed) {
        stance = picked;
        quote = typed == null ? "" : typed;
        return this;
    }

    /**
     * Shows the dialog.
     */
    public void show() {
        host.showDialog(build(), WIDTH);
        if (field != null && !field.isDisabled()) {
            host.stage.setKeyboardFocus(field);
        }
    }

    private Table build() {
        Table card = new Table();
        card.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY, Theme.BORDER_EMPHASIS));
        card.pad(22f, 24f, 20f, 24f);
        card.defaults().growX().left();

        Table head = new Table();
        if (form.crew().isEmpty()) {
            head.add(ui.label(form.title(), Theme.TextStyle.HEADING_LG, Theme.INK)).left();
        } else {
            // A lost flight's name can be long: the title wraps, its note at the end of the first line.
            head.add(ui.paragraph(form.title(), Theme.TextStyle.HEADING_LG, Theme.INK)).left().width(620f);
        }
        if (!form.note().isBlank()) {
            head.add(new HistoryNote(ui, form.note())).size(HistoryNote.ICON_SIZE).padLeft(Theme.SPACE_2).top()
                .padTop(6f);
        }
        head.add().expandX();
        Color status = form.quiet() ? Theme.INK_MUTED : Theme.SUCCESS;
        given.ifPresent(statement -> {
            Image check = new Image(ui.icon("check"), Scaling.fit);
            check.setColor(status);
            head.add(check).size(12f).padRight(6f);
            head.add(ui.label(form.givenPrefix() + ": " + FrontPageView.stanceTitle(statement.stance()),
                Theme.TextStyle.CAPTION, status)).right();
        });
        card.add(head).row();
        card.add(ui.paragraph(form.subtitle(), Theme.TextStyle.BODY, Theme.INK_MUTED)).padTop(4f).padBottom(14f).row();
        if (!form.crew().isEmpty()) {
            card.add(crew()).padBottom(16f).row();
        }

        card.add(ui.label(form.choicesLabel(), Theme.TextStyle.LABEL, Theme.INK_MUTED)).padBottom(6f).row();
        for (FrontPage.Stance option : form.choices()) {
            card.add(row(option)).padBottom(6f).row();
        }

        card.add(ui.label(form.fieldLabel(), Theme.TextStyle.LABEL, Theme.INK_MUTED)).padTop(8f).padBottom(6f).row();
        field = ui.textArea(quote, form.placeholder());
        field.setMaxLength(FrontPage.Statement.MAX_QUOTE_LENGTH);
        field.setTextFieldFilter((textField, c) -> FrontPage.Statement.ALLOWED_CHARACTERS.indexOf(c) >= 0);
        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                quote = field.getText();
                update();
            }
        });
        counter = ui.label("", Theme.TextStyle.CAPTION, Theme.INK_MUTED);
        Table counterLayer = new Table();
        counterLayer.bottom().right().pad(0f, 0f, 6f, 10f);
        counterLayer.add(counter);
        counterLayer.setTouchable(Touchable.disabled);
        Stack box = new Stack();
        box.add(field);
        box.add(counterLayer);
        card.add(box).height(QUOTE_HEIGHT).row();
        error = ui.label("", Theme.TextStyle.CAPTION, Theme.DANGER);
        card.add(error).padTop(4f).row();

        card.add(scrap()).padTop(8f).padBottom(12f).row();
        card.add(ui.paragraph(form.footnote(), Theme.TextStyle.CAPTION, Theme.INK_MUTED)).padBottom(14f).row();

        Table buttons = new Table();
        if (given.isPresent() && form.withdraw()) {
            buttons.add(button("Withdraw", Theme.ButtonKind.DESTRUCTIVE, this::withdraw));
        }
        buttons.add().expandX();
        buttons.add(button("Cancel", Theme.ButtonKind.SECONDARY, host::closeDialog)).padRight(12f);
        submit = button(form.submitLabel(), Theme.ButtonKind.PRIMARY, this::submit);
        buttons.add(submit);
        card.add(buttons).row();
        update();
        return card;
    }

    private static String subtitle(FrontPage first) {
        String agencies = String.join(" and ", first.achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        return "On " + agencies + "’s first " + ContentNames.of(first.goalId()).toLowerCase(java.util.Locale.ROOT)
            + " · printed next season";
    }

    /** The lost crew over the tones: greyscale portraits with a hairline, the name and the age under each. */
    private Table crew() {
        Table row = new Table();
        row.left();
        for (FrontPage.Lost lost : form.crew()) {
            Table person = new Table();
            person.left();
            person.defaults().left();
            Table frame = new Table();
            frame.setBackground(ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
            frame.pad(Theme.BORDER_THIN);
            TextureRegionDrawable portrait = lost.portraitId() == null ? null
                : ui.greyPicture("portraits/" + lost.portraitId() + ".jpg");
            frame.add(portrait == null ? new Image(ui.solid(Theme.SKILL_TRACK)) : new Image(portrait, Scaling.fill))
                .size(64f);
            person.add(frame).padBottom(6f).row();
            person.add(ui.label(lost.name(), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
            person.add(ui.label("Aged " + lost.age(), Theme.TextStyle.CAPTION, Theme.INK_MUTED));
            row.add(person).width(128f).padRight(Theme.SPACE_3).top();
        }
        return row;
    }

    /** One stance row: a radio, the stance's name and its muted line; the picked one outlined. */
    private Table row(FrontPage.Stance option) {
        boolean on = option == stance;
        Table row = new Table();
        // The mockup's choice row: picked, a navy line and a faint navy tint.
        row.setBackground(on ? ui.rect(new Color(Theme.SURFACE_CARD).lerp(Theme.NAVY, 0.05f), Theme.NAVY,
            Theme.BORDER_EMPHASIS) : ui.rect(Theme.SURFACE_CARD, Theme.NAVY_BORDER, Theme.BORDER_THIN));
        row.pad(9f, 12f, 9f, 12f);
        Stack radio = new Stack();
        radio.add(new Image(ui.circle(Theme.SURFACE_CARD, Theme.NAVY, 1.5f, 14)));
        if (on) {
            Table dot = new Table();
            dot.add(new Image(ui.circle(Theme.NAVY, Theme.NAVY, 0f, 7))).size(7f);
            radio.add(dot);
        }
        row.add(radio).size(14f).top().padTop(3f).padRight(12f);
        Table text = new Table();
        text.defaults().left();
        text.add(ui.label(FrontPageView.stanceTitle(option), Theme.TextStyle.BODY_STRONG, Theme.INK)).row();
        text.add(ui.paragraph(FrontPageView.stanceHint(option), Theme.TextStyle.BODY_SM, Theme.INK_MUTED)).growX();
        row.add(text).expandX().fillX();
        row.setTouchable(Touchable.enabled);
        row.addListener(SessionScreen.clicked(() -> {
            stance = option;
            if (!option.quotable()) {
                quote = "";
            }
            show();
        }));
        return row;
    }

    /** The preview: a scrap of the page's paper with how the statement might read. */
    private Table scrap() {
        Table scrap = new Table();
        TextureRegionDrawable paper = ui.picture("frontpage/paper.jpg");
        if (paper == null) {
            scrap.setBackground(ui.solid(Color.valueOf("#f5f0dd")));
        } else {
            // A piece of the page's paper, with no size of its own: the scrap is as tall as its text.
            TextureRegion region = new TextureRegion(paper.getRegion());
            region.setRegion(region.getRegionX() + region.getRegionWidth() / 4, region.getRegionY()
                + region.getRegionHeight() / 3, region.getRegionWidth() / 2, region.getRegionHeight() / 5);
            TextureRegionDrawable piece = new TextureRegionDrawable(region);
            piece.setMinSize(0f, 0f);
            scrap.setBackground(piece);
        }
        scrap.pad(12f, 16f, 14f, 16f);
        Table label = new Table();
        label.add(ui.label("IT MIGHT READ", Theme.TextStyle.NEWS_LABEL_SM, Theme.INK)).left().expandX();
        label.add(ui.label(form.previewDate(), Theme.TextStyle.NEWS_CAPTION, Theme.INK)).right();
        scrap.add(label).growX().padBottom(6f).row();
        Image rule = new Image(ui.solid(Theme.INK));
        rule.setScaling(Scaling.stretch);
        scrap.add(rule).growX().height(1f).padBottom(7f).row();
        preview = ui.paragraph("", Theme.TextStyle.NEWS_QUOTE, Theme.INK);
        preview.setAlignment(Align.left);
        scrap.add(preview).growX();
        return scrap;
    }

    /** Brings the counter, the error, the preview and Submit up to date with the stance and quote. */
    private void update() {
        boolean quotable = stance == null || stance.quotable();
        if (!quotable && !field.getText().isEmpty()) {
            field.setText("");
        }
        field.setDisabled(!quotable);
        String cleaned = FrontPage.Statement.clean(quote);
        String problem = cleaned.length() > FrontPage.Statement.MAX_QUOTE_LENGTH
            ? "At most " + FrontPage.Statement.MAX_QUOTE_LENGTH + " characters."
            : !FrontPage.Statement.allowed(cleaned) ? "Only letters, numbers and common punctuation." : "";
        counter.setText(quote.length() + " / " + FrontPage.Statement.MAX_QUOTE_LENGTH);
        counter.setColor(problem.isEmpty() ? Theme.INK_MUTED : Theme.DANGER);
        ui.setFieldError(field, !problem.isEmpty());
        error.setText(problem);
        error.setVisible(!problem.isEmpty());
        if (stance == null) {
            preview.setText(form.emptyPreview());
            preview.setColor(Theme.INK_MUTED);
        } else {
            preview.setText(form.preview().apply(stance, problem.isEmpty() ? cleaned : ""));
            preview.setColor(Theme.INK);
        }
        submit.setDisabled(stance == null || !problem.isEmpty());
    }

    private void submit() {
        if (stance == null || submit.isDisabled()) {
            return;
        }
        send(stance, FrontPage.Statement.clean(quote), stance.condolence() ? FrontPageView.stanceTitle(stance)
            + " sent." : "Statement given: " + FrontPageView.stanceName(stance) + ".");
    }

    private void withdraw() {
        send(null, "", "Statement withdrawn.");
    }

    private void send(FrontPage.Stance sent, String text, String done) {
        host.game.gateway().submitStatement(host.session.credentials(), host.session.openTurn(), form.pageId(), sent,
            text,
            result -> {
                if (result.ok()) {
                    host.session.keepStatements(result.yours());
                    host.closeDialog();
                    host.toast(done, Theme.NoticeKind.SUCCESS);
                    changed.run();
                } else {
                    host.toast(Refusals.text(result.message()), Theme.NoticeKind.DANGER);
                }
            });
    }

    private TextButton button(String text, Theme.ButtonKind kind, Runnable action) {
        TextButton button = ui.button(text, kind);
        button.addListener(SessionScreen.clicked(() -> {
            if (!button.isDisabled()) {
                action.run();
            }
        }));
        return button;
    }
}
